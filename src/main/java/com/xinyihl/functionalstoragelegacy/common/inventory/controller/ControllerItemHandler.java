package com.xinyihl.functionalstoragelegacy.common.inventory.controller;

import com.xinyihl.functionalstoragelegacy.api.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller inventory handler that aggregates multiple drawer IBigItemHandlers into a single interface.
 * Items are routed to the appropriate sub-handler based on which drawer already contains matching items.
 */
public class ControllerItemHandler implements IBigItemHandler {

    private final List<IBigItemHandler> handlers;
    private final List<HandlerSlotMapping> slotMappings;
    private int totalSlots;

    public ControllerItemHandler() {
        this.handlers = new ArrayList<>();
        this.slotMappings = new ArrayList<>();
        this.totalSlots = 0;
    }

    private void rebuildSlotMappings() {
        slotMappings.clear();
        totalSlots = 0;
        for (int h = 0; h < handlers.size(); h++) {
            IBigItemHandler handler = handlers.get(h);
            for (int s = 0; s < handler.getSlots(); s++) {
                slotMappings.add(new HandlerSlotMapping(h, s));
                totalSlots++;
            }
        }
    }

    @Override
    public int getSlots() {
        return totalSlots;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= totalSlots) return ItemStack.EMPTY;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).getStackInSlot(mapping.slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        long remaining = insertItemLong(slot, stack, stack.getCount(), simulate);
        if (remaining <= 0) return ItemStack.EMPTY;
        if (remaining >= stack.getCount()) return stack;
        ItemStack result = stack.copy();
        result.setCount((int) remaining);
        return result;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= totalSlots || amount <= 0) return ItemStack.EMPTY;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).extractItem(mapping.slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= totalSlots) return 0;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).getSlotLimit(mapping.slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (slot < 0 || slot >= totalSlots) return false;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).isItemValid(mapping.slot, stack);
    }

    public List<IBigItemHandler> getHandlers() {
        return handlers;
    }

    /**
     * Rebuild the handler list from connected drawers.
     */
    public void setHandlers(List<IBigItemHandler> newHandlers) {
        this.handlers.clear();
        this.handlers.addAll(newHandlers);
        rebuildSlotMappings();
    }

    @Override
    public long getLongSlotLimit(int slot) {
        if (slot < 0 || slot >= totalSlots) return 0;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).getLongSlotLimit(mapping.slot);
    }

    @Override
    public long insertItemLong(int slot, @Nonnull ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0) return amount;

        long remaining = amount;

        // Priority 1: Locked handlers with matching items
        for (IBigItemHandler handler : handlers) {
            if (!handler.isLocked()) continue;
            for (int s = 0; s < handler.getRealSlotCount(); s++) {
                ItemStack stored = handler.getStoredType(s);
                if (stored.isEmpty() || !ItemUtil.areItemStacksEqual(stored, stack)) continue;
                remaining = handler.insertItemLong(s, stack, remaining, simulate);
                if (remaining <= 0) return 0;
            }
        }

        // Priority 2: Non-locked handlers with matching items
        for (IBigItemHandler handler : handlers) {
            if (handler.isLocked()) continue;
            for (int s = 0; s < handler.getRealSlotCount(); s++) {
                ItemStack stored = handler.getStoredType(s);
                if (stored.isEmpty() || !ItemUtil.areItemStacksEqual(stored, stack)) continue;
                remaining = handler.insertItemLong(s, stack, remaining, simulate);
                if (remaining <= 0) return 0;
            }
        }

        // Priority 3: Empty slots in non-locked handlers
        for (IBigItemHandler handler : handlers) {
            if (handler.isLocked()) continue;
            for (int s = 0; s < handler.getRealSlotCount(); s++) {
                ItemStack stored = handler.getStoredType(s);
                if (!stored.isEmpty()) continue;
                remaining = handler.insertItemLong(s, stack, remaining, simulate);
                if (remaining <= 0) return 0;
            }
        }

        return remaining;
    }

    @Override
    public long extractItemLong(int slot, long amount, boolean simulate) {
        if (slot < 0 || slot >= totalSlots || amount <= 0) return 0;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).extractItemLong(mapping.slot, amount, simulate);
    }

    @Override
    public long getStoredAmount(int slot) {
        if (slot < 0 || slot >= totalSlots) return 0;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).getStoredAmount(mapping.slot);
    }

    @Nonnull
    @Override
    public ItemStack getStoredType(int slot) {
        if (slot < 0 || slot >= totalSlots) return ItemStack.EMPTY;
        HandlerSlotMapping mapping = slotMappings.get(slot);
        return handlers.get(mapping.handlerIndex).getStoredType(mapping.slot);
    }

    @Override
    public int getRealSlotCount() {
        return totalSlots;
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

    private static class HandlerSlotMapping {
        final int handlerIndex;
        final int slot;

        HandlerSlotMapping(int handlerIndex, int slot) {
            this.handlerIndex = handlerIndex;
            this.slot = slot;
        }
    }
}
