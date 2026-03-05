package com.xinyihl.functionalstoragelgeacy.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Compacting inventory handler. Maintains items at multiple compression tiers.
 * For example: Iron Nugget <-> Iron Ingot <-> Iron Block.
 * Inserting at any tier auto-converts amounts across all tiers.
 */
public abstract class CompactingInventoryHandler implements IItemHandler, ILockable {

    private final List<Result> results;
    private final int slots;
    private boolean setup = false;

    public CompactingInventoryHandler(int slots) {
        this.slots = slots;
        this.results = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            this.results.add(new Result(ItemStack.EMPTY, 1));
        }
    }

    @Override
    public int getSlots() {
        if (isVoid()) return slots + 1;
        return slots;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= slots) return ItemStack.EMPTY;
        Result result = results.get(slot);
        if (result.getStack().isEmpty()) return ItemStack.EMPTY;
        int totalInBase = getTotalInBase();
        int amountAtTier = totalInBase / result.getNeeded();
        if (isCreative() && amountAtTier > 0) amountAtTier = Integer.MAX_VALUE;
        ItemStack out = result.getStack().copy();
        out.setCount(amountAtTier);
        return out;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (isVoid() && slot == slots && isVoidValid(stack)) return ItemStack.EMPTY;

        if (slot < slots) {
            Result result = results.get(slot);
            if (!result.getStack().isEmpty() && areItemStacksEqual(result.getStack(), stack)) {
                int baseEquiv = stack.getCount() * result.getNeeded();
                int maxBase = getSlotLimit(getBaseSlot()) * getBaseResult().getNeeded();
                int currentBase = getTotalInBase();
                int canInsert = Math.min(baseEquiv, maxBase - currentBase);
                int insertedItems = canInsert / result.getNeeded();

                if (insertedItems <= 0) {
                    if (isVoid()) return ItemStack.EMPTY;
                    return stack;
                }

                if (!simulate && !isCreative()) {
                    setTotalInBase(currentBase + insertedItems * result.getNeeded());
                    onChange();
                }

                int leftover = stack.getCount() - insertedItems;
                if (leftover <= 0 || isVoid()) return ItemStack.EMPTY;
                ItemStack rem = stack.copy();
                rem.setCount(leftover);
                return rem;
            }
        }
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot >= slots) return ItemStack.EMPTY;
        Result result = results.get(slot).copy();
        if (result.getStack().isEmpty()) return ItemStack.EMPTY;

        int totalBase = getTotalInBase();
        int available = totalBase / result.getNeeded();
        int toExtract = Math.min(amount, Math.min(available, result.getStack().getMaxStackSize()));

        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate && !isCreative()) {
            setTotalInBase(totalBase - toExtract * result.getNeeded());
            if (!isLocked() && totalInBase <= 0) {
                resetConfiguration();
            }
            onChange();
        }

        ItemStack out = result.getStack().copy();
        out.setCount(toExtract);
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (isCreative()) return Integer.MAX_VALUE;
        if (slot >= slots) return Integer.MAX_VALUE;
        double stackSize = 1;
        Result result = results.get(slot);
        if (!result.getStack().isEmpty()) {
            stackSize = result.getStack().getMaxStackSize() / 64D;
        }
        return (int) Math.floor(getTotalCapacity() * stackSize / result.getNeeded());
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (slot >= slots) return false;
        Result result = results.get(slot);
        if (result.getStack().isEmpty()) return !isLocked();
        return areItemStacksEqual(result.getStack(), stack);
    }

    private boolean isVoidValid(ItemStack stack) {
        for (Result result : results) {
            if (areItemStacksEqual(result.getStack(), stack)) return true;
        }
        return false;
    }

    private static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        return BigInventoryHandler.areItemStacksEqual(a, b);
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("Setup", setup);
        nbt.setInteger("TotalBase", getTotalInBase());
        for (int i = 0; i < results.size(); i++) {
            NBTTagCompound entry = new NBTTagCompound();
            NBTTagCompound stackTag = new NBTTagCompound();
            if (!results.get(i).getStack().isEmpty()) {
                results.get(i).getStack().writeToNBT(stackTag);
            }
            entry.setTag("Stack", stackTag);
            entry.setInteger("Needed", results.get(i).getNeeded());
            nbt.setTag("Result_" + i, entry);
        }
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        setup = nbt.getBoolean("Setup");
        for (int i = 0; i < results.size(); i++) {
            String key = "Result_" + i;
            if (nbt.hasKey(key)) {
                NBTTagCompound entry = nbt.getCompoundTag(key);
                NBTTagCompound stackTag = entry.getCompoundTag("Stack");
                ItemStack stack = stackTag.getKeySet().isEmpty() ? ItemStack.EMPTY : new ItemStack(stackTag);
                results.get(i).setStack(stack);
                results.get(i).setNeeded(entry.getInteger("Needed"));
            }
        }
        setTotalInBase(nbt.getInteger("TotalBase"));
    }

    // Abstract methods
    public abstract void onChange();
    public abstract float getMultiplier();
    public abstract boolean isVoid();
    public abstract boolean isLocked();
    public abstract boolean isCreative();

    // Total capacity in base items
    public double getTotalCapacity() {
        return 64d * getMultiplier();
    }

    // Total stored in base item units
    private int totalInBase = 0;

    public int getTotalInBase() {
        return totalInBase;
    }

    public void setTotalInBase(int totalInBase) {
        this.totalInBase = Math.max(0, totalInBase);
    }

    public List<Result> getResults() {
        return results;
    }

    public boolean isSetup() {
        return setup;
    }

    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    /**
     * Set the compacting results (highest tier first).
     * e.g., [Iron Block(need=81), Iron Ingot(need=9), Iron Nugget(need=1)]
     */
    public void setResults(List<Result> newResults) {
        results.clear();
        results.addAll(newResults);
        setup = true;
    }

    private void resetConfiguration() {
        setTotalInBase(0);
        for (Result result : results) {
            result.setStack(ItemStack.EMPTY);
            result.setNeeded(1);
        }
        setup = false;
    }

    private int getBaseSlot() {
        return results.size() - 1;
    }

    private Result getBaseResult() {
        return results.get(getBaseSlot());
    }

    /**
     * Represents a compacting tier.
     */
    public static class Result {
        private ItemStack stack;
        private int needed;

        public Result(ItemStack stack, int needed) {
            this.stack = stack.copy();
            this.needed = Math.max(1, needed);
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setStack(ItemStack stack) {
            this.stack = stack.copy();
        }

        public int getNeeded() {
            return needed;
        }

        public void setNeeded(int needed) {
            this.needed = Math.max(1, needed);
        }

        public Result copy() {
            return new Result(stack.copy(), needed);
        }
    }
}
