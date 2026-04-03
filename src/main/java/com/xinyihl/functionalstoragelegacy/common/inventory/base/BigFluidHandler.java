package com.xinyihl.functionalstoragelegacy.common.inventory.base;

import com.xinyihl.functionalstoragelegacy.api.IBigFluidHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Big fluid handler supporting multiple tanks with large capacity.
 * Supports locked/void/creative modes like BigInventoryHandler.
 */
public abstract class BigFluidHandler implements IBigFluidHandler {

    private final List<CustomFluidTank> tanks;
    private final int tankCount;
    private IFluidTankProperties[] cachedProperties;

    public BigFluidHandler(int tankCount) {
        this.tankCount = tankCount;
        this.tanks = new ArrayList<>();
        for (int i = 0; i < tankCount; i++) {
            tanks.add(new CustomFluidTank(this::getCapacityPerTank));
        }
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        if (cachedProperties != null) return cachedProperties;
        cachedProperties = new IFluidTankProperties[tankCount];
        for (int i = 0; i < tankCount; i++) {
            final int idx = i;
            cachedProperties[i] = new IFluidTankProperties() {
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
        return cachedProperties;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        long remaining = resource.amount;

        // Priority: matching tanks first
        for (int i = 0; i < tankCount; i++) {
            FluidStack stored = getStoredFluid(i);
            if (stored != null && stored.amount > 0 && stored.isFluidEqual(resource)) {
                long filled = fillLong(i, resource, remaining, !doFill);
                remaining -= filled;
                if (remaining <= 0) return resource.amount;
            }
        }

        // Then empty tanks
        for (int i = 0; i < tankCount; i++) {
            FluidStack stored = getStoredFluid(i);
            if (stored == null || stored.amount <= 0) {
                long filled = fillLong(i, resource, remaining, !doFill);
                remaining -= filled;
                if (remaining <= 0) return resource.amount;
            }
        }

        return (int) Math.min(resource.amount - remaining, Integer.MAX_VALUE);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        long remaining = resource.amount;

        for (int i = 0; i < tankCount; i++) {
            FluidStack stored = getStoredFluid(i);
            if (stored != null && stored.isFluidEqual(resource)) {
                long drained = drainLong(i, remaining, !doDrain);
                remaining -= drained;
                if (remaining <= 0) break;
            }
        }

        long totalDrained = resource.amount - remaining;
        if (totalDrained <= 0) return null;
        FluidStack result = resource.copy();
        result.amount = (int) Math.min(totalDrained, Integer.MAX_VALUE);
        return result;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;

        long remaining = maxDrain;
        FluidStack resultFluid = null;

        for (int i = 0; i < tankCount; i++) {
            FluidStack stored = getStoredFluid(i);
            if (stored != null && stored.amount > 0) {
                if (resultFluid != null && !stored.isFluidEqual(resultFluid)) continue;
                long drained = drainLong(i, remaining, !doDrain);
                if (drained > 0) {
                    if (resultFluid == null) resultFluid = stored.copy();
                    remaining -= drained;
                    if (remaining <= 0) break;
                }
            }
        }

        if (resultFluid == null) return null;
        long totalDrained = maxDrain - remaining;
        if (totalDrained <= 0) return null;
        resultFluid.amount = (int) Math.min(totalDrained, Integer.MAX_VALUE);
        return resultFluid;
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
        return (int) Math.min(getLongCapacityPerTank(), Integer.MAX_VALUE);
    }

    public long getLongCapacityPerTank() {
        if (hasMaxStorage()) {
            return Long.MAX_VALUE;
        }
        float multiplier = getMultiplier();
        if (multiplier <= 0) {
            multiplier = 1.0f;
        }
        return (long) (multiplier * 1000);
    }

    public List<CustomFluidTank> getTanks() {
        return tanks;
    }

    public int getTanksCount() {
        return tankCount;
    }

    @Override
    public long getLongCapacity(int tank) {
        return getLongCapacityPerTank();
    }

    @Override
    public long fillLong(int tank, FluidStack resource, long amount, boolean simulate) {
        if (resource == null || amount <= 0) return 0;
        if (tank < 0 || tank >= tanks.size()) return 0;

        CustomFluidTank target = tanks.get(tank);
        FluidStack current = target.getFluid();

        if (isCreative()) {
            int maxCapacity = getCapacityPerTank();
            if (current == null || current.amount <= 0) {
                if (isLocked() && target.getLockedFluid() != null
                        && !target.getLockedFluid().isFluidEqual(resource)) return 0;
                if (!simulate) {
                    FluidStack full = resource.copy();
                    full.amount = maxCapacity;
                    target.setFluid(full);
                    target.setLockedFluid(resource.copy());
                    onChange();
                }
                return amount;
            }
            if (current.isFluidEqual(resource)) {
                if (!simulate) {
                    current.amount = maxCapacity;
                    target.setFluid(current);
                    onChange();
                }
                return amount;
            }
            return 0;
        }

        if (current != null && current.amount > 0 && !current.isFluidEqual(resource)) return 0;
        if ((current == null || current.amount <= 0) && isLocked()
                && target.getLockedFluid() != null
                && !target.getLockedFluid().isFluidEqual(resource)) return 0;

        long capacity = getLongCapacityPerTank();
        long currentAmount = (current != null) ? current.amount : 0;
        long space = capacity - currentAmount;
        long inserting = Math.min(amount, space);

        if (inserting > 0 && !simulate) {
            if (current == null || current.amount <= 0) {
                FluidStack filled = resource.copy();
                filled.amount = (int) Math.min(inserting, Integer.MAX_VALUE);
                target.setFluid(filled);
            } else {
                current.amount = (int) Math.min(currentAmount + inserting, Integer.MAX_VALUE);
                target.setFluid(current);
            }
            onChange();
        }

        long filled = inserting;
        if (isVoid() && filled < amount) {
            return amount;
        }
        return filled;
    }

    @Override
    public long drainLong(int tank, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        if (tank < 0 || tank >= tanks.size()) return 0;

        CustomFluidTank target = tanks.get(tank);
        FluidStack current = target.getFluid();
        if (current == null || current.amount <= 0) return 0;

        if (isCreative()) return amount;

        long available = current.amount;
        long extracting = Math.min(amount, available);
        if (extracting <= 0) return 0;

        if (!simulate) {
            current.amount -= (int) extracting;
            if (current.amount <= 0) {
                target.setFluid(null);
            }
            onChange();
        }
        return extracting;
    }

    @Nullable
    @Override
    public FluidStack getStoredFluid(int tank) {
        if (tank >= 0 && tank < tanks.size()) {
            return tanks.get(tank).getFluid();
        }
        return null;
    }

    @Override
    public long getStoredFluidAmount(int tank) {
        if (tank >= 0 && tank < tanks.size()) {
            FluidStack fluid = tanks.get(tank).getFluid();
            return fluid != null ? fluid.amount : 0;
        }
        return 0;
    }

    public int fillTank(int tank, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        long filled = fillLong(tank, resource, resource.amount, !doFill);
        return (int) Math.min(filled, Integer.MAX_VALUE);
    }

    @Nullable
    public FluidStack drainTank(int tank, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || tank < 0 || tank >= tanks.size()) return null;
        FluidStack stored = getStoredFluid(tank);
        if (stored == null || stored.amount <= 0) return null;
        FluidStack type = stored.copy();
        long drained = drainLong(tank, maxDrain, !doDrain);
        if (drained <= 0) return null;
        type.amount = (int) Math.min(drained, Integer.MAX_VALUE);
        return type;
    }

    @Nullable
    public FluidStack drainTank(int tank, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0 || tank < 0 || tank >= tanks.size()) return null;
        FluidStack stored = getStoredFluid(tank);
        if (stored == null || !stored.isFluidEqual(resource)) return null;
        long drained = drainLong(tank, resource.amount, !doDrain);
        if (drained <= 0) return null;
        FluidStack result = resource.copy();
        result.amount = (int) Math.min(drained, Integer.MAX_VALUE);
        return result;
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

    protected boolean hasMaxStorage() {
        return false;
    }

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
            return Math.max(0, cap);
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
            return getFluidStack(resource, doDrain, drainedAmount);
        }

        private FluidStack getFluidStack(FluidStack resource, boolean doDrain, int drainedAmount) {
            FluidStack drained = resource.copy();
            drained.amount = drainedAmount;

            if (doDrain) {
                if (fluid != null) {
                    fluid.amount -= drainedAmount;
                    if (fluid.amount <= 0) {
                        fluid = null;
                    }
                    onContentsChanged();
                }
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0 || fluid == null || fluid.amount <= 0) {
                return null;
            }

            int drainedAmount = Math.min(fluid.amount, maxDrain);
            return getFluidStack(fluid, doDrain, drainedAmount);
        }

        public FluidStack getLockedFluid() {
            return lockedFluid;
        }

        public void setLockedFluid(FluidStack lockedFluid) {
            this.lockedFluid = lockedFluid;
        }
    }
}