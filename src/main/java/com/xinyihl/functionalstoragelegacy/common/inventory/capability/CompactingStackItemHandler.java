package com.xinyihl.functionalstoragelegacy.common.inventory.capability;

import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class CompactingStackItemHandler extends CompactingInventoryHandler {

    private final ItemStack drawerStack;
    private final DrawerStackDataHelper.UpgradeState upgradeState;

    public CompactingStackItemHandler(@Nonnull ItemStack drawerStack, int slots) {
        super(slots);
        this.drawerStack = drawerStack;
        this.upgradeState = DrawerStackDataHelper.readUpgradeState(
                DrawerStackDataHelper.getTileData(drawerStack),
                4,
                3
        );
        NBTTagCompound tileData = DrawerStackDataHelper.getTileData(drawerStack);
        if (tileData != null && tileData.hasKey("CompactingInv")) {
            deserializeNBT(tileData.getCompoundTag("CompactingInv"));
        }
    }

    @Override
    public void onChange() {
        NBTTagCompound tileData = DrawerStackDataHelper.getOrCreateTileData(drawerStack);
        tileData.setTag("CompactingInv", serializeNBT());
        tileData.setInteger("SlotCount", getConfiguredSlotCount());
    }

    @Override
    public float getMultiplier() {
        float baseSize = upgradeState.ironDowngrade ? 1.0f : 8.0f;
        return baseSize * upgradeState.storageMultiplier;
    }

    @Override
    protected boolean allowsEquivalentItems() {
        return upgradeState.oreDictionary;
    }

    @Override
    protected boolean hasMaxStorage() {
        return upgradeState.maxStorage;
    }

    @Override
    public boolean isVoid() {
        return upgradeState.voidUpgrade;
    }

    @Override
    public boolean isLocked() {
        return upgradeState.locked;
    }

    @Override
    public boolean isCreative() {
        return upgradeState.creative;
    }

    private int getConfiguredSlotCount() {
        return getResults().size();
    }
}
