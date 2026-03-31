package com.xinyihl.functionalstoragelegacy.common.inventory.controller;

import com.xinyihl.functionalstoragelegacy.api.IBigFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller fluid handler that aggregates multiple drawer IBigFluidHandlers into a single interface.
 */
public class ControllerFluidHandler implements IBigFluidHandler {

    private final List<IBigFluidHandler> handlers;
    private final List<TankSlotMapping> tankMappings;
    private int totalTanks;
    private IFluidTankProperties[] cachedProperties;

    public ControllerFluidHandler() {
        this.handlers = new ArrayList<>();
        this.tankMappings = new ArrayList<>();
        this.totalTanks = 0;
        this.cachedProperties = new IFluidTankProperties[0];
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return cachedProperties;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        long filled = fillLong(0, resource, resource.amount, !doFill);
        return (int) Math.min(filled, Integer.MAX_VALUE);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        long remaining = resource.amount;

        for (IBigFluidHandler handler : handlers) {
            for (int t = 0; t < handler.getTanksCount(); t++) {
                FluidStack stored = handler.getStoredFluid(t);
                if (stored != null && stored.isFluidEqual(resource)) {
                    long drained = handler.drainLong(t, remaining, !doDrain);
                    remaining -= drained;
                    if (remaining <= 0) break;
                }
            }
            if (remaining <= 0) break;
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

        for (IBigFluidHandler handler : handlers) {
            for (int t = 0; t < handler.getTanksCount(); t++) {
                FluidStack stored = handler.getStoredFluid(t);
                if (stored != null && stored.amount > 0) {
                    if (resultFluid != null && !stored.isFluidEqual(resultFluid)) continue;
                    long drained = handler.drainLong(t, remaining, !doDrain);
                    if (drained > 0) {
                        if (resultFluid == null) resultFluid = stored.copy();
                        remaining -= drained;
                        if (remaining <= 0) break;
                    }
                }
            }
            if (remaining <= 0) break;
        }

        if (resultFluid == null) return null;
        long totalDrained = maxDrain - remaining;
        if (totalDrained <= 0) return null;
        resultFluid.amount = (int) Math.min(totalDrained, Integer.MAX_VALUE);
        return resultFluid;
    }

    public List<IBigFluidHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<IBigFluidHandler> newHandlers) {
        this.handlers.clear();
        this.handlers.addAll(newHandlers);
        rebuildTankMappings();
    }

    private void rebuildTankMappings() {
        tankMappings.clear();
        totalTanks = 0;
        for (int h = 0; h < handlers.size(); h++) {
            IBigFluidHandler handler = handlers.get(h);
            for (int t = 0; t < handler.getTanksCount(); t++) {
                tankMappings.add(new TankSlotMapping(h, t));
                totalTanks++;
            }
        }
        List<IFluidTankProperties> props = new ArrayList<>();
        for (IBigFluidHandler handler : handlers) {
            for (IFluidTankProperties prop : handler.getTankProperties()) {
                props.add(prop);
            }
        }
        cachedProperties = props.toArray(new IFluidTankProperties[0]);
    }

    @Override
    public int getTanksCount() {
        return totalTanks;
    }

    @Override
    public long getLongCapacity(int tank) {
        if (tank < 0 || tank >= totalTanks) return 0;
        TankSlotMapping mapping = tankMappings.get(tank);
        return handlers.get(mapping.handlerIndex).getLongCapacity(mapping.localTank);
    }

    @Override
    public long fillLong(int tank, FluidStack resource, long amount, boolean simulate) {
        if (resource == null || amount <= 0) return 0;

        long remaining = amount;

        // Priority 1: Matching tanks
        for (IBigFluidHandler handler : handlers) {
            for (int t = 0; t < handler.getTanksCount(); t++) {
                FluidStack stored = handler.getStoredFluid(t);
                if (stored != null && stored.amount > 0 && stored.isFluidEqual(resource)) {
                    long filled = handler.fillLong(t, resource, remaining, simulate);
                    remaining -= filled;
                    if (remaining <= 0) return amount;
                }
            }
        }

        // Priority 2: Empty tanks
        for (IBigFluidHandler handler : handlers) {
            for (int t = 0; t < handler.getTanksCount(); t++) {
                FluidStack stored = handler.getStoredFluid(t);
                if (stored == null || stored.amount <= 0) {
                    long filled = handler.fillLong(t, resource, remaining, simulate);
                    remaining -= filled;
                    if (remaining <= 0) return amount;
                }
            }
        }

        return amount - remaining;
    }

    @Override
    public long drainLong(int tank, long amount, boolean simulate) {
        if (tank < 0 || tank >= totalTanks) return 0;
        TankSlotMapping mapping = tankMappings.get(tank);
        return handlers.get(mapping.handlerIndex).drainLong(mapping.localTank, amount, simulate);
    }

    @Nullable
    @Override
    public FluidStack getStoredFluid(int tank) {
        if (tank < 0 || tank >= totalTanks) return null;
        TankSlotMapping mapping = tankMappings.get(tank);
        return handlers.get(mapping.handlerIndex).getStoredFluid(mapping.localTank);
    }

    @Override
    public long getStoredFluidAmount(int tank) {
        if (tank < 0 || tank >= totalTanks) return 0;
        TankSlotMapping mapping = tankMappings.get(tank);
        return handlers.get(mapping.handlerIndex).getStoredFluidAmount(mapping.localTank);
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    private static class TankSlotMapping {
        final int handlerIndex;
        final int localTank;

        TankSlotMapping(int handlerIndex, int localTank) {
            this.handlerIndex = handlerIndex;
            this.localTank = localTank;
        }
    }
}
