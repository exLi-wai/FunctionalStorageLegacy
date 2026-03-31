package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.api.IBigFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import net.minecraftforge.fluids.FluidStack;

/**
 * AE2 ME inventory handler aggregating all fluid drawers connected to a controller.
 * Delegates to ControllerFluidHandler's cached handler list for all operations.
 */
public class ControllerMEFluidHandler implements IDrawerMEInventoryHandler<IAEFluidStack> {

    private final ControllerFluidHandler handler;
    private final IFluidStorageChannel channel;

    public ControllerMEFluidHandler(ControllerFluidHandler handler, IFluidStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEFluidStack injectItems(IAEFluidStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        FluidStack inputFluid = input.getFluidStack();
        long filled = handler.fillLong(0, inputFluid, input.getStackSize(), type == Actionable.SIMULATE);
        long remaining = input.getStackSize() - filled;

        if (remaining <= 0) return null;
        if (remaining >= input.getStackSize()) return input;

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
        boolean simulate = mode == Actionable.SIMULATE;

        for (IBigFluidHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getTanksCount(); i++) {
                FluidStack stored = handler.getStoredFluid(i);
                if (stored == null || stored.amount <= 0 || !stored.isFluidEqual(requestFluid)) continue;

                long ext = handler.drainLong(i, toExtract - extracted, simulate);
                extracted += ext;
                if (extracted >= toExtract) break;
            }
            if (extracted >= toExtract) break;
        }

        if (extracted <= 0) return null;

        IAEFluidStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out) {
        for (IBigFluidHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getTanksCount(); i++) {
                FluidStack stored = handler.getStoredFluid(i);
                if (stored == null || stored.amount <= 0) continue;
                long amount = handler.getStoredFluidAmount(i);
                if (amount <= 0) continue;

                IAEFluidStack aeStack = channel.createStack(stored);
                if (aeStack != null) {
                    aeStack.setStackSize(amount);
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
        return false;
    }

    @Override
    public boolean canAccept(IAEFluidStack input) {
        if (input == null) return false;
        FluidStack inputFluid = input.getFluidStack();

        for (IBigFluidHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getTanksCount(); i++) {
                FluidStack stored = handler.getStoredFluid(i);
                if (stored == null || stored.amount <= 0) return true;
                if (stored.isFluidEqual(inputFluid)) {
                    if (handler.getStoredFluidAmount(i) < handler.getLongCapacity(i) || handler.isVoid())
                        return true;
                }
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
