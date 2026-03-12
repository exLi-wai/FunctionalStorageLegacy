package com.xinyihl.functionalstoragelegacy.common.inventory.capability;

import com.xinyihl.functionalstoragelegacy.api.DrawerType;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class DrawerStackItemHandler extends BigInventoryHandler {

    private final ItemStack drawerStack;
    private final DrawerType drawerType;
    private final DrawerStackDataHelper.UpgradeState upgradeState;

    public DrawerStackItemHandler(@Nonnull ItemStack drawerStack, DrawerType drawerType) {
        super(drawerType.getSlots());
        this.drawerStack = drawerStack;
        this.drawerType = drawerType;
        this.upgradeState = DrawerStackDataHelper.readUpgradeState(
                DrawerStackDataHelper.getTileData(drawerStack),
                4,
                3
        );
        NBTTagCompound tileData = DrawerStackDataHelper.getTileData(drawerStack);
        if (tileData != null && tileData.hasKey("Inventory")) {
            deserializeNBT(tileData.getCompoundTag("Inventory"));
        }
    }

    @Override
    public void onChange() {
        NBTTagCompound tileData = DrawerStackDataHelper.getOrCreateTileData(drawerStack);
        tileData.setTag("Inventory", serializeNBT());
    }

    @Override
    public float getMultiplier() {
        float baseSize = upgradeState.ironDowngrade ? 1.0f : drawerType.getSlotAmount();
        return baseSize * upgradeState.storageMultiplier;
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
}
