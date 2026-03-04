package com.xinyihl.functionalstoragelgeacy.block.tile;

import com.xinyihl.functionalstoragelgeacy.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.util.CompactingUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * TileEntity for compacting drawers (3-slot compression storage).
 * Handles nugget <-> ingot <-> block style compaction.
 */
public class CompactingDrawerTile extends ControllableDrawerTile {

    private static final HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    private CompactingInventoryHandler handler;
    private boolean hasCheckedRecipes;
    private int removeTicks = 0;

    public CompactingDrawerTile() {
        this(3);
    }

    public CompactingDrawerTile(int slots) {
        super();
        this.handler = createHandler(slots);
        this.hasCheckedRecipes = false;
    }

    protected CompactingInventoryHandler createHandler(int slots) {
        return new CompactingInventoryHandler(slots) {
            @Override
            public void onChange() {
                CompactingDrawerTile.this.markDirty();
                CompactingDrawerTile.this.sendUpdatePacket();
            }

            @Override
            public float getMultiplier() {
                float baseSize = CompactingDrawerTile.this.hasIronDowngrade() ? 1.0f : 8.0f;
                return baseSize * CompactingDrawerTile.this.getStorageMultiplier();
            }

            @Override
            public boolean isVoid() {
                return CompactingDrawerTile.this.isVoid();
            }

            @Override
            public boolean isCreative() {
                return CompactingDrawerTile.this.isCreative();
            }

            @Override
            public boolean isLocked() {
                return CompactingDrawerTile.this.isLocked();
            }
        };
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote) {
            removeTicks = Math.max(removeTicks - 1, 0);

            // Check recipes on first tick
            if (!hasCheckedRecipes) {
                if (handler.isSetup() && !getParentStack().isEmpty()) {
                    List<CompactingInventoryHandler.Result> results = CompactingUtil.getCompactingResults(this.world, getParentStack(), getSlotCount());
                    if (!results.isEmpty()) {
                        applyCompactingResults(results);
                    }
                }
                hasCheckedRecipes = true;
            }
        }
    }

    /**
     * Get "parent" item stack - the item used to set up compaction.
     * Returns the base tier item if setup, empty otherwise.
     */
    private ItemStack getParentStack() {
        List<CompactingInventoryHandler.Result> results = handler.getResults();
        if (results.isEmpty()) return ItemStack.EMPTY;
        // Return the highest tier non-empty result
        for (CompactingInventoryHandler.Result result : results) {
            if (!result.getStack().isEmpty()) return result.getStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ, int slot) {
        ItemStack heldStack = player.getHeldItem(hand);

        if (super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot)) {
            return true;
        }

        if (slot != -1 && !world.isRemote) {
            // Setup compacting if not yet configured
            if (!handler.isSetup() && !heldStack.isEmpty()) {
                ItemStack template = heldStack.copy();
                template.setCount(1);
                List<CompactingInventoryHandler.Result> results = CompactingUtil.getCompactingResults(this.world, template, getSlotCount());
                if (!results.isEmpty()) {
                    applyCompactingResults(results);
                    markDirty();
                    sendUpdatePacket();
                }
            }

            // Insert items
            if (!heldStack.isEmpty() && handler.isSetup()) {
                ItemStack result = handler.insertItem(slot, heldStack, true);
                if (result.getCount() != heldStack.getCount()) {
                    player.setHeldItem(hand, handler.insertItem(slot, heldStack, false));
                    return true;
                }
            }

            // Double-click fast insert
            if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(player.getUniqueID(), System.currentTimeMillis()) < 300 && (isLocked() || !handler.getStackInSlot(0).isEmpty())) {
                for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                    ItemStack invStack = player.inventory.getStackInSlot(i);
                    if (!invStack.isEmpty()) {
                        ItemStack testResult = handler.insertItem(slot, invStack, true);
                        if (testResult.getCount() != invStack.getCount()) {
                            ItemStack leftover = handler.insertItem(slot, invStack.copy(), false);
                            player.inventory.setInventorySlotContents(i, leftover);
                        }
                    }
                }
            }

            INTERACTION_LOGGER.put(player.getUniqueID(), System.currentTimeMillis());
        }

        return true;
    }

    @Override
    public void onClicked(EntityPlayer player, int slot) {
        if (!world.isRemote && slot != -1 && removeTicks == 0) {
            removeTicks = 3;
            int amount = player.isSneaking() ? handler.getStackInSlot(slot).getMaxStackSize() : 1;
            ItemStack extracted = handler.extractItem(slot, amount, false);
            if (!extracted.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, extracted);
            }
        }
    }

    private void applyCompactingResults(List<CompactingInventoryHandler.Result> compactingResults) {
        // Pad or trim to match slots
        List<CompactingInventoryHandler.Result> results = new ArrayList<>(compactingResults);
        while (results.size() < getSlotCount()) {
            results.add(new CompactingInventoryHandler.Result(ItemStack.EMPTY, 1));
        }
        while (results.size() > getSlotCount()) {
            results.remove(results.size() - 1);
        }
        handler.setResults(results);
    }

    protected int getSlotCount() {
        return 3;
    }

    public CompactingInventoryHandler getCompactingHandler() {
        return handler;
    }

    @Override
    protected void writeCustomData(NBTTagCompound nbt) {
        nbt.setTag("CompactingInv", handler.serializeNBT());
        nbt.setInteger("SlotCount", getSlotCount());
    }

    @Override
    protected void readCustomData(NBTTagCompound nbt) {
        int slots = nbt.hasKey("SlotCount") ? nbt.getInteger("SlotCount") : getSlotCount();
        handler = createHandler(slots);
        if (nbt.hasKey("CompactingInv")) {
            handler.deserializeNBT(nbt.getCompoundTag("CompactingInv"));
        }
        hasCheckedRecipes = false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        compound.setTag("CompactingInv", handler.serializeNBT());
        compound.setInteger("SlotCount", getSlotCount());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        int slots = compound.hasKey("SlotCount") ? compound.getInteger("SlotCount") : getSlotCount();
        handler = createHandler(slots);
        super.readFromNBT(compound);
        if (compound.hasKey("CompactingInv")) {
            handler.deserializeNBT(compound.getCompoundTag("CompactingInv"));
        }
        hasCheckedRecipes = false;
    }

    @Override
    public IItemHandler getItemHandler() {
        return handler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean isEverythingEmpty() {
        if (!super.isEverythingEmpty()) return false;
        return handler.getTotalInBase() == 0 && !handler.isSetup();
    }

    @Override
    protected int calculateRedstoneSignal() {
        if (!handler.isSetup()) return 0;
        int totalCapacity = 0;
        int totalStored = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            totalCapacity += handler.getSlotLimit(i);
            totalStored += handler.getStackInSlot(i).getCount();
        }
        if (totalCapacity == 0) return 0;
        return (int) ((totalStored / (double) totalCapacity) * 15);
    }

    @Override
    public int getStorageSlotAmount() {
        return 3;
    }

    public CompactingInventoryHandler getHandler() {
        return handler;
    }
}
