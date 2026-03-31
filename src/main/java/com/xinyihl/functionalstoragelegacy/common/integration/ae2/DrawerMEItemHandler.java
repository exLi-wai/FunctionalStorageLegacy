package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.api.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;

/**
 * AE2 ME inventory handler wrapping an IBigItemHandler for item drawers.
 * Supports long-level item amounts via IBigItemHandler interface.
 */
public class DrawerMEItemHandler implements IDrawerMEInventoryHandler<IAEItemStack> {

    private final IBigItemHandler handler;
    private final IItemStorageChannel channel;

    public DrawerMEItemHandler(IBigItemHandler handler, IItemStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        ItemStack inputStack = input.getDefinition();
        long remaining = input.getStackSize();
        boolean simulate = type == Actionable.SIMULATE;

        // Priority 1: Matching slots
        for (int i = 0; i < handler.getRealSlotCount(); i++) {
            ItemStack stored = handler.getStoredType(i);
            if (stored.isEmpty() || !ItemUtil.areItemStacksEqual(stored, inputStack)) continue;
            remaining = handler.insertItemLong(i, inputStack, remaining, simulate);
            if (remaining <= 0) return null;
        }

        // Priority 2: Empty slots
        for (int i = 0; i < handler.getRealSlotCount(); i++) {
            ItemStack stored = handler.getStoredType(i);
            if (!stored.isEmpty()) continue;
            remaining = handler.insertItemLong(i, inputStack, remaining, simulate);
            if (remaining <= 0) return null;
        }

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

        for (int i = 0; i < handler.getRealSlotCount(); i++) {
            ItemStack stored = handler.getStoredType(i);
            if (stored.isEmpty() || !ItemUtil.areItemStacksEqual(stored, requestStack)) continue;

            long ext = handler.extractItemLong(i, toExtract - extracted, simulate);
            extracted += ext;
            if (extracted >= toExtract) break;
        }

        if (extracted <= 0) return null;

        IAEItemStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
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
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();
        for (int i = 0; i < handler.getRealSlotCount(); i++) {
            ItemStack stored = handler.getStoredType(i);
            if (!stored.isEmpty() && ItemUtil.areItemStacksEqual(stored, inputStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();

        for (int i = 0; i < handler.getRealSlotCount(); i++) {
            ItemStack stored = handler.getStoredType(i);
            if (stored.isEmpty() && !handler.isLocked()) return true;
            if (!stored.isEmpty() && ItemUtil.areItemStacksEqual(stored, inputStack)) {
                if (handler.isCreative() || handler.isVoid()) return true;
                if (handler.getStoredAmount(i) < handler.getLongSlotLimit(i)) return true;
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
