package com.xinyihl.functionalstoragelegacy.common.inventory.base;

import com.xinyihl.functionalstoragelegacy.api.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.api.ILockable;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Core large-capacity item storage handler.
 * Each slot can store items beyond vanilla's 64 limit using BigStack (ItemStack + long amount).
 * Supports locked/void/creative modes.
 */
public abstract class BigInventoryHandler implements IBigItemHandler, ILockable {

    public static final String BIG_ITEMS = "BigItems";
    public static final String STACK = "Stack";
    public static final String AMOUNT = "Amount";

    private final int slots;
    private final List<BigStack> storedStacks;

    public BigInventoryHandler(int slots) {
        this.slots = slots;
        this.storedStacks = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            this.storedStacks.add(new BigStack(ItemStack.EMPTY, 0));
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
        if (slots == slot) return ItemStack.EMPTY;
        BigStack bigStack = this.storedStacks.get(slot);
        if (isCreative()) {
            ItemStack copy = bigStack.getStack().copy();
            copy.setCount(Integer.MAX_VALUE);
            return copy;
        }
        return bigStack.getSlotStack();
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
        if (amount <= 0 || slot < 0 || slot >= slots) return ItemStack.EMPTY;
        BigStack bigStack = this.storedStacks.get(slot);
        if (bigStack.getStack().isEmpty()) return ItemStack.EMPTY;
        ItemStack type = bigStack.getStack().copy();
        amount = Math.min(amount, type.getMaxStackSize());
        long extracted = extractItemLong(slot, amount, simulate);
        if (extracted <= 0) return ItemStack.EMPTY;
        type.setCount((int) Math.min(extracted, Integer.MAX_VALUE));
        return type;
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) Math.min(getLongSlotLimit(slot), Integer.MAX_VALUE);
    }

    public long getLongSlotLimit(int slot) {
        if (isCreative()) return Long.MAX_VALUE;
        if (slots == slot) return Long.MAX_VALUE;
        double stackSize = 1;
        if (!getStoredStacks().get(slot).getStack().isEmpty()) {
            stackSize = getStoredStacks().get(slot).getStack().getMaxStackSize() / 64D;
        }
        return (long) Math.floor(getTotalAmount() * stackSize);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    public long insertItemLong(int slot, @Nonnull ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0) return amount;

        if (isVoid() && slots == slot && isVoidValid(stack)) return 0;

        if (isCreative() && slot < slots) {
            BigStack bigStack = this.storedStacks.get(slot);
            if (bigStack.getStack().isEmpty()) {
                if (!simulate) {
                    ItemStack template = stack.copy();
                    template.setCount(stack.getMaxStackSize());
                    bigStack.setStack(template);
                    bigStack.setAmount(Long.MAX_VALUE);
                    onChange();
                }
                return 0;
            }
            if (ItemUtil.areItemStacksCompatible(bigStack.getStack(), stack, allowsEquivalentItems())) {
                if (!simulate) {
                    bigStack.setAmount(Long.MAX_VALUE);
                    onChange();
                }
                return 0;
            }
            return amount;
        }

        if (isValid(slot, stack)) {
            BigStack bigStack = this.storedStacks.get(slot);
            long limit = getLongSlotLimit(slot);
            long inserted = Math.max(0, Math.min(limit - bigStack.getAmount(), amount));
            if (inserted == 0 && !isVoid()) return amount;
            if (!simulate) {
                if (bigStack.getStack().isEmpty()) {
                    ItemStack template = stack.copy();
                    template.setCount(stack.getMaxStackSize());
                    bigStack.setStack(template);
                }
                if (inserted > 0) {
                    bigStack.setAmount(Math.min(bigStack.getAmount() + inserted, limit));
                }
                onChange();
            }
            long remaining = amount - inserted;
            if (remaining > 0 && isVoid()) return 0;
            return remaining;
        }
        return amount;
    }

    @Override
    public long extractItemLong(int slot, long amount, boolean simulate) {
        if (amount <= 0 || slot >= slots || slot < 0) return 0;
        BigStack bigStack = this.storedStacks.get(slot);
        if (bigStack.getStack().isEmpty()) return 0;

        if (isCreative()) return amount;

        long available = bigStack.getAmount();
        long extracting = Math.min(amount, available);
        if (extracting <= 0) return 0;

        if (!simulate) {
            bigStack.setAmount(available - extracting);
            if (bigStack.getAmount() <= 0 && !isLocked()) {
                bigStack.setStack(ItemStack.EMPTY);
            }
            onChange();
        }
        return extracting;
    }

    @Override
    public long getStoredAmount(int slot) {
        if (slot >= 0 && slot < storedStacks.size()) {
            return storedStacks.get(slot).getAmount();
        }
        return 0;
    }

    @Nonnull
    @Override
    public ItemStack getStoredType(int slot) {
        if (slot >= 0 && slot < storedStacks.size()) {
            return storedStacks.get(slot).getStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getRealSlotCount() {
        return slots;
    }

    private boolean isValid(int slot, @Nonnull ItemStack stack) {
        if (slot < slots) {
            BigStack bigStack = this.storedStacks.get(slot);
            ItemStack fl = bigStack.getStack();
            if (isLocked() && fl.isEmpty()) return false;
            return fl.isEmpty() || ItemUtil.areItemStacksCompatible(fl, stack, allowsEquivalentItems());
        }
        return false;
    }

    private boolean isVoidValid(ItemStack stack) {
        for (BigStack storedStack : this.storedStacks) {
            if (ItemUtil.areItemStacksCompatible(storedStack.getStack(), stack, allowsEquivalentItems())) return true;
        }
        return false;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagCompound items = new NBTTagCompound();
        for (int i = 0; i < this.storedStacks.size(); i++) {
            NBTTagCompound bigStack = new NBTTagCompound();
            NBTTagCompound stackTag = new NBTTagCompound();
            if (!this.storedStacks.get(i).getStack().isEmpty()) {
                this.storedStacks.get(i).getStack().writeToNBT(stackTag);
            }
            bigStack.setTag(STACK, stackTag);
            bigStack.setLong(AMOUNT, this.storedStacks.get(i).getAmount());
            items.setTag(String.valueOf(i), bigStack);
        }
        compoundTag.setTag(BIG_ITEMS, items);
        return compoundTag;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(BIG_ITEMS)) return;
        NBTTagCompound bigItems = nbt.getCompoundTag(BIG_ITEMS);
        for (String key : bigItems.getKeySet()) {
            int index = Integer.parseInt(key);
            if (index < this.storedStacks.size()) {
                NBTTagCompound entry = bigItems.getCompoundTag(key);
                NBTTagCompound stackTag = entry.getCompoundTag(STACK);
                ItemStack stack = stackTag.getKeySet().isEmpty() ? ItemStack.EMPTY : new ItemStack(stackTag);
                this.storedStacks.get(index).setStack(stack);
                this.storedStacks.get(index).setAmount(entry.getLong(AMOUNT));
            }
        }
    }

    public abstract void onChange();

    public abstract float getMultiplier();

    public long getTotalAmount() {
        if (hasMaxStorage()) {
            return Long.MAX_VALUE;
        }
        return (long) (64d * getMultiplier());
    }

    protected boolean allowsEquivalentItems() {
        return false;
    }

    protected boolean hasMaxStorage() {
        return false;
    }

    public abstract boolean isVoid();

    public abstract boolean isLocked();

    public abstract boolean isCreative();

    public List<BigStack> getStoredStacks() {
        return storedStacks;
    }

    public BigStack getBigStack(int slot) {
        if (slot >= 0 && slot < storedStacks.size()) {
            return storedStacks.get(slot);
        }
        return new BigStack(ItemStack.EMPTY, 0);
    }

    public int getSlotCount() {
        return slots;
    }

    /**
     * A stack that can hold more than 64 items.
     */
    public static class BigStack {

        private ItemStack stack;
        private ItemStack slotStack;
        private long amount;

        public BigStack(ItemStack stack, long amount) {
            this.amount = amount;
            if (!stack.isEmpty()) {
                this.stack = stack.copy();
                if (amount > 0) {
                    this.slotStack = stack.copy();
                    this.slotStack.setCount((int) Math.min(amount, Integer.MAX_VALUE));
                } else {
                    this.slotStack = ItemStack.EMPTY;
                }
            } else {
                this.stack = ItemStack.EMPTY;
                this.slotStack = ItemStack.EMPTY;
            }
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setStack(ItemStack stack) {
            if (!stack.isEmpty()) {
                this.stack = stack.copy();
                if (this.amount > 0) {
                    this.slotStack = this.stack.copy();
                    this.slotStack.setCount((int) Math.min(this.amount, Integer.MAX_VALUE));
                } else {
                    this.slotStack = ItemStack.EMPTY;
                }
            } else {
                this.stack = ItemStack.EMPTY;
                this.slotStack = ItemStack.EMPTY;
            }
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = Math.max(amount, 0);
            if (!this.stack.isEmpty() && this.amount > 0) {
                if (this.slotStack.isEmpty()) {
                    this.slotStack = this.stack.copy();
                }
                this.slotStack.setCount((int) Math.min(this.amount, Integer.MAX_VALUE));
            } else {
                this.slotStack = ItemStack.EMPTY;
            }
        }

        public ItemStack getSlotStack() {
            return slotStack;
        }
    }
}
