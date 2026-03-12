package com.xinyihl.functionalstoragelegacy.common.inventory.controller;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller fluid handler that aggregates multiple drawer IFluidHandlers into a single interface.
 */
public class ControllerFluidHandler implements IFluidHandler {

    private final List<IFluidHandler> handlers;

    public ControllerFluidHandler() {
        this.handlers = new ArrayList<>();
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        List<IFluidTankProperties> props = new ArrayList<>();
        for (IFluidHandler handler : handlers) {
            props.addAll(Arrays.asList(handler.getTankProperties()));
        }
        return props.toArray(new IFluidTankProperties[0]);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        int remaining = resource.amount;

        // Priority: Fill into handlers that already have matching fluid
        for (IFluidHandler handler : handlers) {
            for (IFluidTankProperties prop : handler.getTankProperties()) {
                FluidStack contents = prop.getContents();
                if (contents != null && contents.isFluidEqual(resource)) {
                    FluidStack toFill = resource.copy();
                    toFill.amount = remaining;
                    int filled = handler.fill(toFill, doFill);
                    remaining -= filled;
                    if (remaining <= 0) return resource.amount;
                }
            }
        }

        // Then fill into empty slots
        for (IFluidHandler handler : handlers) {
            for (IFluidTankProperties prop : handler.getTankProperties()) {
                FluidStack contents = prop.getContents();
                if (contents == null || contents.amount == 0) {
                    FluidStack toFill = resource.copy();
                    toFill.amount = remaining;
                    int filled = handler.fill(toFill, doFill);
                    remaining -= filled;
                    if (remaining <= 0) return resource.amount;
                }
            }
        }

        return resource.amount - remaining;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        int remaining = resource.amount;
        FluidStack result = null;

        for (IFluidHandler handler : handlers) {
            FluidStack toDrain = resource.copy();
            toDrain.amount = remaining;
            FluidStack drained = handler.drain(toDrain, doDrain);
            if (drained != null && drained.amount > 0) {
                if (result == null) {
                    result = drained.copy();
                } else {
                    result.amount += drained.amount;
                }
                remaining -= drained.amount;
                if (remaining <= 0) break;
            }
        }

        return result;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;

        int remaining = maxDrain;
        FluidStack result = null;

        for (IFluidHandler handler : handlers) {
            FluidStack drained = handler.drain(remaining, doDrain);
            if (drained != null && drained.amount > 0) {
                if (result != null && !result.isFluidEqual(drained)) continue;
                if (result == null) {
                    result = drained.copy();
                } else {
                    result.amount += drained.amount;
                }
                remaining -= drained.amount;
                if (remaining <= 0) break;
            }
        }

        return result;
    }

    public List<IFluidHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<IFluidHandler> newHandlers) {
        this.handlers.clear();
        this.handlers.addAll(newHandlers);
    }
}
