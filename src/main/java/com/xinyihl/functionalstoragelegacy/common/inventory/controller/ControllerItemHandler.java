package com.xinyihl.functionalstoragelegacy.common.inventory.controller;

import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller inventory handler that aggregates multiple drawer IItemHandlers into a single interface.
 * Items are routed to the appropriate sub-handler based on which drawer already contains matching items.
 */
public class ControllerItemHandler implements IItemHandler {

    private final List<IItemHandler> handlers;
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
            IItemHandler handler = handlers.get(h);
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

        // Priority 1: Insert into handlers that already have this item
        ItemStack remaining = stack.copy();
        for (IItemHandler handler : handlers) {
            for (int s = 0; s < handler.getSlots(); s++) {
                ItemStack existing = handler.getStackInSlot(s);
                if (!existing.isEmpty() && ItemUtil.areItemStacksEqual(existing, remaining)) {
                    remaining = handler.insertItem(s, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // Priority 2: Insert into empty slots
        for (IItemHandler handler : handlers) {
            for (int s = 0; s < handler.getSlots(); s++) {
                ItemStack existing = handler.getStackInSlot(s);
                if (existing.isEmpty() && handler.isItemValid(s, remaining)) {
                    remaining = handler.insertItem(s, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
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

    public List<IItemHandler> getHandlers() {
        return handlers;
    }

    /**
     * Rebuild the handler list from connected drawers.
     */
    public void setHandlers(List<IItemHandler> newHandlers) {
        this.handlers.clear();
        this.handlers.addAll(newHandlers);
        rebuildSlotMappings();
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
