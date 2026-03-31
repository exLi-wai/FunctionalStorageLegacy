package com.xinyihl.functionalstoragelegacy.common.inventory.capability;

import com.xinyihl.functionalstoragelegacy.api.DrawerType;
import com.xinyihl.functionalstoragelegacy.api.upgrade.ModifierType;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;

public class FluidDrawerStackItemHandler extends BigFluidHandler implements IFluidHandlerItem {

    private final ItemStack drawerStack;
    private final DrawerType drawerType;
    private final DrawerStackDataHelper.UpgradeState upgradeState;

    public FluidDrawerStackItemHandler(@Nonnull ItemStack drawerStack, DrawerType drawerType) {
        super(drawerType.getSlots());
        this.drawerStack = drawerStack;
        this.drawerType = drawerType;
        this.upgradeState = DrawerStackDataHelper.readUpgradeState(
                DrawerStackDataHelper.getTileData(drawerStack),
                4,
                3
        );
        NBTTagCompound tileData = DrawerStackDataHelper.getTileData(drawerStack);
        if (tileData != null && tileData.hasKey("FluidInv")) {
            deserializeNBT(tileData.getCompoundTag("FluidInv"));
        }
    }

    @Override
    public void onChange() {
        NBTTagCompound tileData = DrawerStackDataHelper.getOrCreateTileData(drawerStack);
        tileData.setTag("FluidInv", serializeNBT());
    }

    @Override
    public float getMultiplier() {
        return upgradeState.calculate(ModifierType.FLUID_STORAGE, drawerType.getSlotAmount());
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

    @Nonnull
    @Override
    public ItemStack getContainer() {
        return drawerStack;
    }
}
