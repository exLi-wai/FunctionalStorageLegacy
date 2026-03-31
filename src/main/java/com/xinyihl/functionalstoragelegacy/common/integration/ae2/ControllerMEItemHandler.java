package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.api.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerItemHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;

/**
 * AE2 ME inventory handler aggregating all item drawers connected to a controller.
 * Delegates to ControllerItemHandler's cached handler list for all operations.
 */
public class ControllerMEItemHandler implements IDrawerMEInventoryHandler<IAEItemStack> {

    private final ControllerItemHandler handler;
    private final IItemStorageChannel channel;

    public ControllerMEItemHandler(ControllerItemHandler handler, IItemStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        ItemStack inputStack = input.getDefinition();
        long remaining = handler.insertItemLong(0, inputStack, input.getStackSize(), type == Actionable.SIMULATE);

        if (remaining <= 0) return null;
        if (remaining >= input.getStackSize()) return input;

        IAEItemStack result = input.copy();
        result.setStackSize(remaining);
        return result;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;

        ItemStack requestStack = request.getDefinition();
        long toExtract = request.getStackSize();
        long extracted = 0;
        boolean simulate = mode == Actionable.SIMULATE;

        for (IBigItemHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getRealSlotCount(); i++) {
                ItemStack stored = handler.getStoredType(i);
                if (stored.isEmpty() || !ItemUtil.areItemStacksEqual(stored, requestStack)) continue;

                long ext = handler.extractItemLong(i, toExtract - extracted, simulate);
                extracted += ext;
                if (extracted >= toExtract) break;
            }
            if (extracted >= toExtract) break;
        }

        if (extracted <= 0) return null;

        IAEItemStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (IBigItemHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getRealSlotCount(); i++) {
                ItemStack stored = handler.getStoredType(i);
                if (stored.isEmpty()) continue;
                long amount = handler.getStoredAmount(i);
                if (amount <= 0) continue;

                IAEItemStack aeStack = channel.createStack(stored);
                if (aeStack != null) {
                    aeStack.setStackSize(amount);
                    out.addStorage(aeStack);
                }
            }
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();

        for (IBigItemHandler handler : handler.getHandlers()) {
            for (int i = 0; i < handler.getRealSlotCount(); i++) {
                ItemStack stored = handler.getStoredType(i);
                if (stored.isEmpty() && !handler.isLocked()) return true;
                if (!stored.isEmpty() && ItemUtil.areItemStacksEqual(stored, inputStack)) {
                    if (handler.getStoredAmount(i) < handler.getLongSlotLimit(i) || handler.isVoid())
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
