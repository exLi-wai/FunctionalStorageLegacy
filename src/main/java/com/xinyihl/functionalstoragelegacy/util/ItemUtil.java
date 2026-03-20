package com.xinyihl.functionalstoragelegacy.util;

import com.xinyihl.functionalstoragelegacy.common.item.upgrade.StorageUpgradeItem;
import com.xinyihl.functionalstoragelegacy.common.item.upgrade.UtilityUpgradeItem;
import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemUtil {

    /**
     * Check if two ItemStacks are the same item with same metadata and NBT (ignoring count).
     */
    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata() && ItemStack.areItemStackTagsEqual(a, b);
    }

    public static boolean isStorageUpgradeItem(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof StorageUpgradeItem || stack.getItem() == RegistrationHandler.CREATIVE_VENDING_UPGRADE;
    }

    public static boolean isUtilityUpgradeItem(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof UtilityUpgradeItem;
    }
}
