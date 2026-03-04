package com.xinyihl.functionalstoragelgeacy.fluid;

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
                    return true;
                }

                @Override
                public boolean canDrainFluidType(FluidStack fluidStack) {
                    return true;
                }
            };
        }
        return props;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        // Try to fill into existing matching tanks first
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

        // Then try empty tanks
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

        int totalFilled = resource.amount - remaining + (resource.amount - remaining < resource.amount && isVoid() ? remaining : 0);
        if (totalFilled > 0 && doFill) onChange();
        return isVoid() ? resource.amount : (resource.amount - remaining);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

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
        return (int) (getMultiplier() * 1000);
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
        if (filled > 0 && doFill) onChange();
        if (isVoid()) return resource.amount;
        return filled;
    }

    @Nullable
    public FluidStack drainTank(int tank, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;
        if (tank < 0 || tank >= tanks.size()) return null;

        CustomFluidTank target = tanks.get(tank);
        FluidStack drained = target.drain(maxDrain, doDrain);
        if (drained != null && drained.amount > 0 && doDrain) onChange();
        return drained;
    }

    @Nullable
    public FluidStack drainTank(int tank, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        if (tank < 0 || tank >= tanks.size()) return null;

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
        private FluidStack lockedFluid;
        private final java.util.function.IntSupplier capacitySupplier;

        public CustomFluidTank(java.util.function.IntSupplier capacitySupplier) {
            super(Integer.MAX_VALUE);
            this.capacitySupplier = capacitySupplier;
        }

        @Override
        public int getCapacity() {
            return capacitySupplier.getAsInt();
        }

        public FluidStack getLockedFluid() {
            return lockedFluid;
        }

        public void setLockedFluid(FluidStack lockedFluid) {
            this.lockedFluid = lockedFluid;
        }
    }
}
