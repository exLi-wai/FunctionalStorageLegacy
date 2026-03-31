package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler.CustomFluidTank;
import net.minecraftforge.fluids.FluidStack;

/**
 * AE2 ME inventory handler wrapping a BigFluidHandler for fluid drawers.
 * Supports long-level fluid amounts via IAEFluidStack.
 */
public class DrawerMEFluidHandler implements IDrawerMEInventoryHandler<IAEFluidStack> {

    private final BigFluidHandler handler;
    private final IFluidStorageChannel channel;

    public DrawerMEFluidHandler(BigFluidHandler handler, IFluidStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEFluidStack injectItems(IAEFluidStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        FluidStack inputFluid = input.getFluidStack();
        long toInsert = input.getStackSize();
        long remaining = toInsert;

        if (handler.isCreative()) {
            for (CustomFluidTank tank : handler.getTanks()) {
                FluidStack current = tank.getFluid();
                if (current == null || current.amount <= 0) {
                    if (handler.isLocked() && tank.getLockedFluid() != null
                            && !tank.getLockedFluid().isFluidEqual(inputFluid)) continue;
                    if (type == Actionable.MODULATE) {
                        FluidStack full = inputFluid.copy();
                        full.amount = handler.getCapacityPerTank();
                        tank.setFluid(full);
                        tank.setLockedFluid(inputFluid.copy());
                        handler.onChange();
                    }
                    return null;
                }
                if (current.isFluidEqual(inputFluid)) {
                    if (type == Actionable.MODULATE) {
                        current.amount = handler.getCapacityPerTank();
                        tank.setFluid(current);
                        handler.onChange();
                    }
                    return null;
                }
            }
            return input;
        }

        // Fill into matching tanks first
        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current != null && current.amount > 0 && current.isFluidEqual(inputFluid)) {
                int capacity = tank.getCapacity();
                long space = capacity - current.amount;
                long inserting = Math.min(remaining, space);
                if (inserting > 0) {
                    if (type == Actionable.MODULATE) {
                        current.amount += (int) inserting;
                        tank.setFluid(current);
                        handler.onChange();
                    }
                    remaining -= inserting;
                    if (remaining <= 0) return null;
                }
            }
        }

        // Fill into empty tanks
        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current == null || current.amount <= 0) {
                if (handler.isLocked() && tank.getLockedFluid() != null
                        && !tank.getLockedFluid().isFluidEqual(inputFluid)) continue;
                int capacity = tank.getCapacity();
                long inserting = Math.min(remaining, capacity);
                if (inserting > 0) {
                    if (type == Actionable.MODULATE) {
                        FluidStack filled = inputFluid.copy();
                        filled.amount = (int) inserting;
                        tank.setFluid(filled);
                        handler.onChange();
                    }
                    remaining -= inserting;
                    if (remaining <= 0) return null;
                }
            }
        }

        // Void absorbs remaining
        if (handler.isVoid() && remaining > 0 && remaining < toInsert) {
            return null;
        }

        if (remaining >= toInsert) return input;

        IAEFluidStack result = input.copy();
        result.setStackSize(remaining);
        return result;
    }

    @Override
    public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, IActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;

        FluidStack requestFluid = request.getFluidStack();
        long toExtract = request.getStackSize();
        long extracted = 0;

        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current != null && current.amount > 0 && current.isFluidEqual(requestFluid)) {
                long available = handler.isCreative() ? toExtract : current.amount;
                long extracting = Math.min(toExtract - extracted, available);
                if (extracting > 0) {
                    if (mode == Actionable.MODULATE && !handler.isCreative()) {
                        current.amount -= (int) extracting;
                        if (current.amount <= 0) {
                            tank.setFluid(null);
                        }
                        handler.onChange();
                    }
                    extracted += extracting;
                    if (extracted >= toExtract) break;
                }
            }
        }

        if (extracted <= 0) return null;

        IAEFluidStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out) {
        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current != null && current.amount > 0) {
                IAEFluidStack aeStack = channel.createStack(current);
                if (aeStack != null) {
                    aeStack.setStackSize(handler.isCreative() ? Long.MAX_VALUE : current.amount);
                    out.addStorage(aeStack);
                }
            }
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEFluidStack> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEFluidStack input) {
        if (input == null) return false;
        FluidStack inputFluid = input.getFluidStack();
        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current != null && current.amount > 0 && current.isFluidEqual(inputFluid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAccept(IAEFluidStack input) {
        if (input == null) return false;
        FluidStack inputFluid = input.getFluidStack();

        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();
            if (current == null || current.amount <= 0) {
                if (handler.isLocked() && tank.getLockedFluid() != null) {
                    if (tank.getLockedFluid().isFluidEqual(inputFluid)) return true;
                    continue;
                }
                return true;
            }
            if (current.isFluidEqual(inputFluid)) {
                if (current.amount < tank.getCapacity()) return true;
                if (handler.isVoid()) return true;
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int pass) {
        return true;
    }
}
