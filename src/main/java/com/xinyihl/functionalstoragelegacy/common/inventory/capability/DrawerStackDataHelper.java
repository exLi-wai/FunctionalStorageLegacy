package com.xinyihl.functionalstoragelegacy.common.inventory.capability;

import com.xinyihl.functionalstoragelegacy.api.upgrade.ModifierType;
import com.xinyihl.functionalstoragelegacy.api.upgrade.UpgradeModifier;
import com.xinyihl.functionalstoragelegacy.common.item.upgrade.StorageUpgradeItem;
import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

final class DrawerStackDataHelper {

    private DrawerStackDataHelper() {
    }

    @Nullable
    static NBTTagCompound getTileData(@Nonnull ItemStack drawerStack) {
        if (!drawerStack.hasTagCompound() || !drawerStack.getTagCompound().hasKey("TileData")) {
            return null;
        }
        return drawerStack.getTagCompound().getCompoundTag("TileData");
    }

    @Nonnull
    static NBTTagCompound getOrCreateTileData(@Nonnull ItemStack drawerStack) {
        if (!drawerStack.hasTagCompound()) {
            drawerStack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound stackTag = drawerStack.getTagCompound();
        if (!stackTag.hasKey("TileData")) {
            stackTag.setTag("TileData", new NBTTagCompound());
        }
        return stackTag.getCompoundTag("TileData");
    }

    @Nonnull
    static UpgradeState readUpgradeState(@Nullable NBTTagCompound tileData, int storageUpgradeSlots, int utilityUpgradeSlots) {
        UpgradeState state = new UpgradeState();
        if (tileData == null) {
            return state;
        }

        if (tileData.hasKey("Locked")) {
            state.locked = tileData.getBoolean("Locked");
        }

        if (tileData.hasKey("StorageUpgrades")) {
            ItemStackHandler storageUpgrades = new ItemStackHandler(storageUpgradeSlots);
            storageUpgrades.deserializeNBT(tileData.getCompoundTag("StorageUpgrades"));
            for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                ItemStack upgradeStack = storageUpgrades.getStackInSlot(i);
                if (upgradeStack.isEmpty()) {
                    continue;
                }
                if (upgradeStack.getItem() instanceof StorageUpgradeItem) {
                    StorageUpgradeItem upgrade = (StorageUpgradeItem) upgradeStack.getItem();
                    if (upgrade.isMaxStorageUpgrade()) {
                        state.maxStorage = true;
                    } else {
                        state.addModifiers(upgrade.getModifiers());
                    }
                }
                if (upgradeStack.getItem() == RegistrationHandler.CREATIVE_VENDING_UPGRADE) {
                    state.creative = true;
                }
            }
        }

        if (tileData.hasKey("UtilityUpgrades")) {
            ItemStackHandler utilityUpgrades = new ItemStackHandler(utilityUpgradeSlots);
            utilityUpgrades.deserializeNBT(tileData.getCompoundTag("UtilityUpgrades"));
            for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
                if (utilityUpgrades.getStackInSlot(i).getItem() == RegistrationHandler.VOID_UPGRADE) {
                    state.voidUpgrade = true;
                }
                if (utilityUpgrades.getStackInSlot(i).getItem() == RegistrationHandler.ORE_DICTIONARY_UPGRADE) {
                    state.oreDictionary = true;
                }
            }
        }

        return state;
    }

    static final class UpgradeState {
        private final Map<ModifierType, List<UpgradeModifier>> modifiers = new EnumMap<>(ModifierType.class);
        boolean maxStorage = false;
        boolean creative = false;
        boolean voidUpgrade = false;
        boolean locked = false;
        boolean oreDictionary = false;

        void addModifiers(Map<ModifierType, UpgradeModifier> mods) {
            for (Map.Entry<ModifierType, UpgradeModifier> entry : mods.entrySet()) {
                modifiers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        float calculate(ModifierType type, float defaultBase) {
            List<UpgradeModifier> mods = modifiers.get(type);
            if (mods == null || mods.isEmpty()) {
                return UpgradeModifier.calculate(defaultBase);
            }
            return UpgradeModifier.calculate(mods, defaultBase);
        }
    }
}
