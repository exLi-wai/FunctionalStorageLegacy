package com.xinyihl.functionalstoragelegacy.api;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

/**
 * Extended item handler interface supporting long-level throughput.
 * Extends IItemHandler to maintain compatibility with Forge's capability system.
 */
public interface IBigItemHandler extends IItemHandler {

    /**
     * Get the capacity of a slot as a long value.
     */
    long getLongSlotLimit(int slot);

    /**
     * Insert items into a specific slot with long-level amounts.
     *
     * @param slot     The slot to insert into
     * @param stack    The item type to insert
     * @param amount   The amount to insert (long)
     * @param simulate If true, the insertion is only simulated
     * @return The amount that was NOT inserted (remaining)
     */
    long insertItemLong(int slot, @Nonnull ItemStack stack, long amount, boolean simulate);

    /**
     * Extract items from a specific slot with long-level amounts.
     *
     * @param slot     The slot to extract from
     * @param amount   The maximum amount to extract (long)
     * @param simulate If true, the extraction is only simulated
     * @return The amount that was actually extracted
     */
    long extractItemLong(int slot, long amount, boolean simulate);

    /**
     * Get the stored amount in a slot as a long value.
     */
    long getStoredAmount(int slot);

    /**
     * Get the item type stored in a slot (without quantity information).
     */
    @Nonnull
    ItemStack getStoredType(int slot);

    /**
     * Get the real slot count, excluding virtual slots like the void pseudo-slot.
     */
    int getRealSlotCount();

    /**
     * Whether this handler has the void upgrade (absorbs excess items).
     */
    boolean isVoid();

    /**
     * Whether this handler is locked (only accepts matching items, retains type when empty).
     */
    boolean isLocked();

    /**
     * Whether this handler is in creative mode (infinite storage).
     */
    boolean isCreative();
}
