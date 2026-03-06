package com.xinyihl.functionalstoragelgeacy.container;

import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * Container for drawer upgrade management GUI.
 * Shows storage upgrade slots and utility upgrade slots.
 */
public class ContainerDrawer extends Container {

    private final ControllableDrawerTile tile;

    public ContainerDrawer(InventoryPlayer playerInventory, ControllableDrawerTile tile) {
        this.tile = tile;

        // Storage upgrade slots (top row)
        ItemStackHandler storageUpgrades = tile.getStorageUpgrades();
        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            addSlotToContainer(new SlotItemHandler(storageUpgrades, i, 8 + i * 18, 20));
        }

        // Utility upgrade slots (second row)
        ItemStackHandler utilityUpgrades = tile.getUtilityUpgrades();
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            addSlotToContainer(new SlotItemHandler(utilityUpgrades, i, 8 + i * 18, 44));
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            int upgradeSlots = tile.getStorageUpgrades().getSlots() + tile.getUtilityUpgrades().getSlots();

            if (index < upgradeSlots) {
                // Transfer from upgrade slot to player inventory
                if (!mergeItemStack(stackInSlot, upgradeSlots, upgradeSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Transfer from player inventory to upgrade slots
                if (!mergeItemStack(stackInSlot, 0, upgradeSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public ControllableDrawerTile getTile() {
        return tile;
    }
}
