package com.xinyihl.functionalstoragelegacy.common.inventory;

import com.xinyihl.functionalstoragelegacy.misc.FunctionalStorageConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

/**
 * Inventory handler for the Armory Cabinet.
 * Each slot holds exactly 1 unstackable item (armor, weapons, tools, music discs, etc.)
 */
public abstract class ArmoryCabinetInventoryHandler implements IItemHandler {

    private final ItemStack[] stacks;
    private final int size;

    public ArmoryCabinetInventoryHandler() {
        this(FunctionalStorageConfig.ARMORY_CABINET_SIZE);
    }

    public ArmoryCabinetInventoryHandler(int size) {
        this.size = size;
        this.stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
    }

    public abstract void onChange();

    @Override
    public int getSlots() {
        return size;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= size) return ItemStack.EMPTY;
        return stacks[slot];
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;

        // Try to find an empty slot if the given slot is occupied
        int targetSlot = slot;
        if (!stacks[targetSlot].isEmpty()) {
            targetSlot = -1;
            for (int i = 0; i < size; i++) {
                if (stacks[i].isEmpty()) {
                    targetSlot = i;
                    break;
                }
            }
            if (targetSlot == -1) return stack;
        }

        if (!simulate) {
            ItemStack toInsert = stack.copy();
            toInsert.setCount(1);
            stacks[targetSlot] = toInsert;
            onChange();
        }

        if (stack.getCount() == 1) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        return remainder;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= size || amount <= 0 || stacks[slot].isEmpty()) return ItemStack.EMPTY;

        ItemStack out = stacks[slot].copy();
        if (!simulate) {
            stacks[slot] = ItemStack.EMPTY;
            onChange();
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Accept unstackable items: damageable, enchantable, music discs, horse armor, etc.
        return stack.getMaxStackSize() == 1 || stack.isItemStackDamageable();
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("Size", size);
        for (int i = 0; i < size; i++) {
            if (!stacks[i].isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                stacks[i].writeToNBT(itemTag);
                nbt.setTag("Slot_" + i, itemTag);
            }
        }
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        for (int i = 0; i < size; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
        for (String key : nbt.getKeySet()) {
            if (key.startsWith("Slot_")) {
                int index = Integer.parseInt(key.substring(5));
                if (index >= 0 && index < size) {
                    stacks[index] = new ItemStack(nbt.getCompoundTag(key));
                }
            }
        }
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }
}
