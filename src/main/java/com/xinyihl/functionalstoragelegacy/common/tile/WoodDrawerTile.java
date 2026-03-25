package com.xinyihl.functionalstoragelegacy.common.tile;

import com.xinyihl.functionalstoragelegacy.api.DrawerType;
import com.xinyihl.functionalstoragelegacy.api.DrawerWoodType;
import com.xinyihl.functionalstoragelegacy.api.UpgradeState;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * TileEntity for standard wooden drawer blocks.
 * Holds a BigInventoryHandler for large-capacity item storage.
 */
public class WoodDrawerTile extends ControllableDrawerTile {

    private static final HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    private BigInventoryHandler handler;
    private DrawerType drawerType;
    private DrawerWoodType woodType;
    private int removeTicks = 0;

    public WoodDrawerTile() {
        this(DrawerType.X_1, DrawerWoodType.OAK);
    }

    public WoodDrawerTile(DrawerType drawerType, DrawerWoodType woodType) {
        super();
        this.drawerType = drawerType;
        this.woodType = woodType;
        this.handler = createHandler();
    }

    private BigInventoryHandler createHandler() {
        return new BigInventoryHandler(drawerType.getSlots()) {
            @Override
            public void onChange() {
                WoodDrawerTile.this.markDirty();
                WoodDrawerTile.this.sendUpdatePacket();
            }

            @Override
            public float getMultiplier() {
                float baseSize = WoodDrawerTile.this.hasIronDowngrade() ? 1.0f : WoodDrawerTile.this.drawerType.getSlotAmount();
                return baseSize * WoodDrawerTile.this.getStorageMultiplier();
            }

            @Override
            protected boolean allowsEquivalentItems() {
                return WoodDrawerTile.this.hasOreDictionaryUpgrade();
            }

            @Override
            protected boolean hasMaxStorage() {
                return WoodDrawerTile.this.hasMaxStorageUpgrade();
            }

            @Override
            public boolean isVoid() {
                return WoodDrawerTile.this.isVoid();
            }

            @Override
            public boolean isLocked() {
                return WoodDrawerTile.this.isLocked();
            }

            @Override
            public boolean isCreative() {
                return WoodDrawerTile.this.isCreative();
            }
        };
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote) {
            removeTicks = Math.max(removeTicks - 1, 0);
        }
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ, int slot) {
        ItemStack heldStack = player.getHeldItem(hand);

        // Let parent handle upgrades and tools
        if (super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot)) {
            return true;
        }

        if (slot != -1 && !world.isRemote) {
            // Set the type filter if empty slot and holding item
            if (!heldStack.isEmpty() && slot < handler.getSlotCount()) {
                BigInventoryHandler.BigStack bigStack = handler.getStoredStacks().get(slot);
                if (bigStack.getStack().isEmpty()) {
                    ItemStack template = heldStack.copy();
                    template.setCount(heldStack.getMaxStackSize());
                    bigStack.setStack(template);
                }
            }

            // Try to insert held item
            if (!heldStack.isEmpty()) {
                ItemStack result = handler.insertItem(slot, heldStack, true);
                if (result.getCount() != heldStack.getCount()) {
                    player.setHeldItem(hand, handler.insertItem(slot, heldStack, false));
                    return true;
                }
            }

            // Double-click fast insert from inventory
            if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(player.getUniqueID(), System.currentTimeMillis()) < 300 && (isLocked() || !handler.getStackInSlot(slot).isEmpty())) {
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

    @Override
    protected void writeCustomData(NBTTagCompound nbt) {
        nbt.setTag("Inventory", handler.serializeNBT());
        nbt.setInteger("DrawerType", drawerType.ordinal());
        nbt.setInteger("WoodType", woodType.ordinal());
    }

    @Override
    protected void readCustomData(NBTTagCompound nbt) {
        if (nbt.hasKey("DrawerType")) {
            drawerType = DrawerType.values()[nbt.getInteger("DrawerType")];
        }
        if (nbt.hasKey("WoodType")) {
            woodType = DrawerWoodType.values()[nbt.getInteger("WoodType")];
        }
        handler = createHandler();
        if (nbt.hasKey("Inventory")) {
            handler.deserializeNBT(nbt.getCompoundTag("Inventory"));
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        compound.setTag("Inventory", handler.serializeNBT());
        compound.setInteger("DrawerType", drawerType.ordinal());
        compound.setInteger("WoodType", woodType.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        if (compound.hasKey("DrawerType")) {
            drawerType = DrawerType.values()[compound.getInteger("DrawerType")];
        }
        if (compound.hasKey("WoodType")) {
            woodType = DrawerWoodType.values()[compound.getInteger("WoodType")];
        }
        handler = createHandler();
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            handler.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
    }

    @Override
    public IItemHandler getItemHandler() {
        return handler;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean isEverythingEmpty() {
        if (!super.isEverythingEmpty()) return false;
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigInventoryHandler.BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() || bs.getAmount() > 0) return false;
        }
        return true;
    }

    @Override
    protected int calculateRedstoneSignal() {
        int totalCapacity = 0;
        int totalStored = 0;
        for (int i = 0; i < handler.getSlotCount(); i++) {
            totalCapacity += handler.getSlotLimit(i);
            totalStored += handler.getStoredStacks().get(i).getAmount();
        }
        if (totalCapacity == 0) return 0;
        return (int) ((totalStored / (double) totalCapacity) * 15);
    }

    @Override
    protected boolean canApplyUpgradeState(UpgradeState state) {
        if (state.creative || state.maxStorage) {
            return true;
        }
        float baseSize = state.ironDowngrade ? 1.0f : drawerType.getSlotAmount();
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigInventoryHandler.BigStack bigStack = handler.getStoredStacks().get(i);
            if (bigStack.getAmount() <= 0) {
                continue;
            }
            double stackSize = 1.0d;
            if (!bigStack.getStack().isEmpty()) {
                stackSize = bigStack.getStack().getMaxStackSize() / 64D;
            }
            int capacity = (int) Math.floor(64D * baseSize * state.storageMultiplier * stackSize);
            if (bigStack.getAmount() > capacity) {
                return false;
            }
        }
        return true;
    }

    public BigInventoryHandler getHandler() {
        return handler;
    }

    public DrawerType getDrawerType() {
        return drawerType;
    }

    public DrawerWoodType getWoodType() {
        return woodType;
    }
}
