package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler.BigStack;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * AE2 ME inventory handler wrapping a BigInventoryHandler for item drawers.
 * Supports long-level item amounts by directly accessing BigStack.
 */
public class DrawerMEItemHandler implements IDrawerMEInventoryHandler<IAEItemStack> {

    private final BigInventoryHandler handler;
    private final IItemStorageChannel channel;

    public DrawerMEItemHandler(BigInventoryHandler handler, IItemStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        ItemStack inputStack = input.getDefinition();
        long toInsert = input.getStackSize();
        long remaining = toInsert;

        if (handler.isCreative()) {
            for (int i = 0; i < handler.getSlotCount(); i++) {
                BigStack bs = handler.getStoredStacks().get(i);
                if (bs.getStack().isEmpty()) {
                    if (handler.isLocked()) continue;
                    if (type == Actionable.MODULATE) {
                        ItemStack template = inputStack.copy();
                        template.setCount(inputStack.getMaxStackSize());
                        bs.setStack(template);
                        bs.setAmount(Long.MAX_VALUE);
                        handler.onChange();
                    }
                    return null;
                }
                if (isCompatible(bs.getStack(), inputStack)) {
                    if (type == Actionable.MODULATE) {
                        bs.setAmount(Long.MAX_VALUE);
                        handler.onChange();
                    }
                    return null;
                }
            }
            return input;
        }

        // Normal insertion: match existing slots first, then empty slots
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() && isCompatible(bs.getStack(), inputStack)) {
                long limit = handler.getLongSlotLimit(i);
                long space = limit - bs.getAmount();
                long inserting = Math.min(remaining, space);
                if (inserting > 0) {
                    if (type == Actionable.MODULATE) {
                        bs.setAmount(bs.getAmount() + inserting);
                        handler.onChange();
                    }
                    remaining -= inserting;
                    if (remaining <= 0) return null;
                }
            }
        }

        // Try empty slots
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (bs.getStack().isEmpty()) {
                if (handler.isLocked()) continue;
                long limit = handler.getLongSlotLimit(i);
                long inserting = Math.min(remaining, limit);
                if (inserting > 0) {
                    if (type == Actionable.MODULATE) {
                        ItemStack template = inputStack.copy();
                        template.setCount(inputStack.getMaxStackSize());
                        bs.setStack(template);
                        bs.setAmount(inserting);
                        handler.onChange();
                    }
                    remaining -= inserting;
                    if (remaining <= 0) return null;
                }
            }
        }

        // Void upgrade absorbs remaining
        if (handler.isVoid() && remaining > 0 && remaining < toInsert) {
            return null;
        }

        if (remaining >= toInsert) return input;

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

        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() && isCompatible(bs.getStack(), requestStack)) {
                long available = handler.isCreative() ? toExtract : bs.getAmount();
                long extracting = Math.min(toExtract - extracted, available);
                if (extracting > 0) {
                    if (mode == Actionable.MODULATE && !handler.isCreative()) {
                        bs.setAmount(bs.getAmount() - extracting);
                        if (bs.getAmount() <= 0 && !handler.isLocked()) {
                            bs.setStack(ItemStack.EMPTY);
                        }
                        handler.onChange();
                    }
                    extracted += extracting;
                    if (extracted >= toExtract) break;
                }
            }
        }

        if (extracted <= 0) return null;

        IAEItemStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() && bs.getAmount() > 0) {
                IAEItemStack aeStack = channel.createStack(bs.getStack());
                if (aeStack != null) {
                    aeStack.setStackSize(handler.isCreative() ? Long.MAX_VALUE : bs.getAmount());
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
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() && isCompatible(bs.getStack(), inputStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();

        if (handler.isCreative()) {
            for (int i = 0; i < handler.getSlotCount(); i++) {
                BigStack bs = handler.getStoredStacks().get(i);
                if (bs.getStack().isEmpty() && !handler.isLocked()) return true;
                if (!bs.getStack().isEmpty() && isCompatible(bs.getStack(), inputStack)) return true;
            }
            return false;
        }

        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (bs.getStack().isEmpty()) {
                if (!handler.isLocked()) return true;
                continue;
            }
            if (isCompatible(bs.getStack(), inputStack)) {
                if (bs.getAmount() < handler.getLongSlotLimit(i)) return true;
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

    private boolean isCompatible(@Nonnull ItemStack stored, @Nonnull ItemStack input) {
        return ItemUtil.areItemStacksEqual(stored, input);
    }
}
