package com.xinyihl.functionalstoragelegacy.common.inventory.base;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Big fluid handler supporting multiple tanks with large capacity.
 * Supports locked/void/creative modes like BigInventoryHandler.
 */
public abstract class BigFluidHandler implements IFluidHandler {

    private final List<CustomFluidTank> tanks;
    private final int tankCount;

    public BigFluidHandler(int tankCount) {
        this.tankCount = tankCount;
        this.tanks = new ArrayList<>();
        for (int i = 0; i < tankCount; i++) {
            tanks.add(new CustomFluidTank(this::getCapacityPerTank));
        }
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] props = new IFluidTankProperties[tankCount];
        for (int i = 0; i < tankCount; i++) {
            final int idx = i;
            props[i] = new IFluidTankProperties() {
                @Nullable
                @Override
                public FluidStack getContents() {
                    return tanks.get(idx).getFluid();
                }

                @Override
                public int getCapacity() {
                    return getCapacityPerTank();
                }

                @Override
                public boolean canFill() {
                    return true;
                }

                @Override
                public boolean canDrain() {
                    return true;
                }

                @Override
                public boolean canFillFluidType(FluidStack fluidStack) {
                    CustomFluidTank tank = tanks.get(idx);
                    FluidStack current = tank.getFluid();
                    if (current != null && current.amount > 0) {
                        return current.isFluidEqual(fluidStack);
                    }
                    if (isLocked() && tank.getLockedFluid() != null) {
                        return tank.getLockedFluid().isFluidEqual(fluidStack);
                    }
                    return true;
                }

                @Override
                public boolean canDrainFluidType(FluidStack fluidStack) {
                    CustomFluidTank tank = tanks.get(idx);
                    FluidStack current = tank.getFluid();
                    if (current != null) {
                        return current.isFluidEqual(fluidStack);
                    }
                    return false;
                }
            };
        }
        return props;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        if (isCreative()) {
            if (doFill) onChange();
            return resource.amount;
        }

        int remaining = resource.amount;
        for (CustomFluidTank tank : tanks) {
            if (tank.getFluid() != null && tank.getFluid().isFluidEqual(resource)) {
                int filled = tank.fill(resource.copy(), doFill);
                remaining -= filled;
                if (remaining <= 0) break;
                FluidStack next = resource.copy();
                next.amount = remaining;
                resource = next;
            }
        }

        if (remaining > 0) {
            for (CustomFluidTank tank : tanks) {
                if (tank.getFluid() == null || tank.getFluid().amount == 0) {
                    if (isLocked() && tank.getLockedFluid() != null && !tank.getLockedFluid().isFluidEqual(resource))
                        continue;
                    FluidStack toFill = resource.copy();
                    toFill.amount = remaining;
                    int filled = tank.fill(toFill, doFill);
                    remaining -= filled;
                    if (remaining <= 0) break;
                }
            }
        }

        int totalFilled = resource.amount - remaining;

        if (isVoid() && totalFilled < resource.amount) {
            if (doFill) onChange();
            return resource.amount;
        }

        if (totalFilled > 0 && doFill) onChange();
        return totalFilled;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        if (isCreative()) {
            for (CustomFluidTank tank : tanks) {
                if (tank.getFluid() != null && tank.getFluid().isFluidEqual(resource)) {
                    if (doDrain) onChange();
                    return resource.copy();
                }
            }
            return null;
        }

        int remaining = resource.amount;
        FluidStack result = null;

        for (CustomFluidTank tank : tanks) {
            if (tank.getFluid() != null && tank.getFluid().isFluidEqual(resource)) {
                FluidStack drained = tank.drain(remaining, doDrain);
                if (drained != null) {
                    if (result == null) {
                        result = drained.copy();
                    } else {
                        result.amount += drained.amount;
                    }
                    remaining -= drained.amount;
                    if (remaining <= 0) break;
                }
            }
        }

        if (result != null && doDrain) onChange();
        return result;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;

        if (isCreative()) {
            for (CustomFluidTank tank : tanks) {
                if (tank.getFluid() != null && tank.getFluid().amount > 0) {
                    if (doDrain) onChange();
                    FluidStack fluid = tank.getFluid().copy();
                    fluid.amount = maxDrain;
                    return fluid;
                }
            }
            return null;
        }

        FluidStack result = null;
        int remaining = maxDrain;

        for (CustomFluidTank tank : tanks) {
            if (tank.getFluid() != null && tank.getFluid().amount > 0) {
                if (result != null && !tank.getFluid().isFluidEqual(result)) continue;
                FluidStack drained = tank.drain(remaining, doDrain);
                if (drained != null) {
                    if (result == null) {
                        result = drained.copy();
                    } else {
                        result.amount += drained.amount;
                    }
                    remaining -= drained.amount;
                    if (remaining <= 0) break;
                }
            }
        }

        if (result != null && doDrain) onChange();
        return result;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < tanks.size(); i++) {
            NBTTagCompound tankTag = new NBTTagCompound();
            tanks.get(i).writeToNBT(tankTag);
            if (tanks.get(i).getLockedFluid() != null) {
                NBTTagCompound lockedTag = new NBTTagCompound();
                tanks.get(i).getLockedFluid().writeToNBT(lockedTag);
                tankTag.setTag("LockedFluid", lockedTag);
            }
            nbt.setTag("Tank_" + i, tankTag);
        }
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        for (int i = 0; i < tanks.size(); i++) {
            String key = "Tank_" + i;
            if (nbt.hasKey(key)) {
                NBTTagCompound tankTag = nbt.getCompoundTag(key);
                tanks.get(i).readFromNBT(tankTag);
                if (tankTag.hasKey("LockedFluid")) {
                    tanks.get(i).setLockedFluid(FluidStack.loadFluidStackFromNBT(tankTag.getCompoundTag("LockedFluid")));
                }
            }
        }
    }

    public int getCapacityPerTank() {
        float multiplier = getMultiplier();
        if (multiplier <= 0 || multiplier > 100000) {
            multiplier = 1.0f;
        }
        return (int) Math.min(multiplier * 1000, Integer.MAX_VALUE / 2);
    }

    public List<CustomFluidTank> getTanks() {
        return tanks;
    }

    public int getTanksCount() {
        return tankCount;
    }

    public int fillTank(int tank, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        if (tank < 0 || tank >= tanks.size()) return 0;

        if (isCreative()) {
            if (doFill) onChange();
            return resource.amount;
        }

        CustomFluidTank target = tanks.get(tank);
        FluidStack current = target.getFluid();

        if (current != null && current.amount > 0 && !current.isFluidEqual(resource)) {
            return 0;
        }

        if ((current == null || current.amount <= 0) && isLocked()
                && target.getLockedFluid() != null
                && !target.getLockedFluid().isFluidEqual(resource)) {
            return 0;
        }

        int filled = target.fill(resource.copy(), doFill);

        if (isVoid()) {
            if (doFill) onChange();
            return resource.amount;
        }

        if (filled > 0 && doFill) onChange();
        return filled;
    }

    @Nullable
    public FluidStack drainTank(int tank, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;
        if (tank < 0 || tank >= tanks.size()) return null;

        if (isCreative()) {
            CustomFluidTank target = tanks.get(tank);
            if (target.getFluid() != null) {
                FluidStack result = target.getFluid().copy();
                result.amount = maxDrain;
                if (doDrain) onChange();
                return result;
            }
            return null;
        }

        CustomFluidTank target = tanks.get(tank);
        FluidStack drained = target.drain(maxDrain, doDrain);
        if (drained != null && drained.amount > 0 && doDrain) onChange();
        return drained;
    }

    @Nullable
    public FluidStack drainTank(int tank, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        if (tank < 0 || tank >= tanks.size()) return null;

        if (isCreative()) {
            CustomFluidTank target = tanks.get(tank);
            if (target.getFluid() != null && target.getFluid().isFluidEqual(resource)) {
                FluidStack result = resource.copy();
                if (doDrain) onChange();
                return result;
            }
            return null;
        }

        CustomFluidTank target = tanks.get(tank);
        FluidStack current = target.getFluid();
        if (current == null || !current.isFluidEqual(resource)) return null;

        FluidStack drained = target.drain(resource.amount, doDrain);
        if (drained != null && drained.amount > 0 && doDrain) onChange();
        return drained;
    }

    @Nullable
    public FluidStack getTankFluid(int tank) {
        if (tank >= 0 && tank < tanks.size()) {
            return tanks.get(tank).getFluid();
        }
        return null;
    }

    public abstract void onChange();

    public abstract float getMultiplier();

    public abstract boolean isVoid();

    public abstract boolean isLocked();

    public abstract boolean isCreative();

    /**
     * Custom fluid tank with locking support.
     */
    public static class CustomFluidTank extends FluidTank {
        private final java.util.function.IntSupplier capacitySupplier;
        private FluidStack lockedFluid;

        public CustomFluidTank(java.util.function.IntSupplier capacitySupplier) {
            super(0);
            this.capacitySupplier = capacitySupplier;
        }

        @Override
        public int getCapacity() {
            int cap = capacitySupplier.getAsInt();
            return Math.max(0, Math.min(cap, Integer.MAX_VALUE / 2));
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) {
                return 0;
            }

            if (fluid != null && fluid.amount > 0 && !fluid.isFluidEqual(resource)) {
                return 0;
            }

            int capacity = getCapacity();
            int space = capacity - (fluid != null ? fluid.amount : 0);

            if (space <= 0) {
                return 0;
            }

            int filled = Math.min(space, resource.amount);

            if (doFill && filled > 0) {
                if (fluid == null) {
                    fluid = resource.copy();
                    fluid.amount = filled;
                } else {
                    fluid.amount += filled;
                }
                onContentsChanged();
            }

            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0 || fluid == null || fluid.amount <= 0) {
                return null;
            }

            if (!fluid.isFluidEqual(resource)) {
                return null;
            }

            int drainedAmount = Math.min(fluid.amount, resource.amount);
            FluidStack drained = resource.copy();
            drained.amount = drainedAmount;

            if (doDrain) {
                fluid.amount -= drainedAmount;
                if (fluid.amount <= 0) {
                    fluid = null;
                }
                onContentsChanged();
            }

            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0 || fluid == null || fluid.amount <= 0) {
                return null;
            }

            int drainedAmount = Math.min(fluid.amount, maxDrain);
            FluidStack drained = fluid.copy();
            drained.amount = drainedAmount;

            if (doDrain) {
                fluid.amount -= drainedAmount;
                if (fluid.amount <= 0) {
                    fluid = null;
                }
                onContentsChanged();
            }

            return drained;
        }

        public FluidStack getLockedFluid() {
            return lockedFluid;
        }

        public void setLockedFluid(FluidStack lockedFluid) {
            this.lockedFluid = lockedFluid;
        }
    }
}