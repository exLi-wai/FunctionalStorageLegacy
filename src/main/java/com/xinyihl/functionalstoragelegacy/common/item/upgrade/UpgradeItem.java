package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for all drawer upgrade items.
 */
public abstract class UpgradeItem extends Item {

    private final Type type;
    private final Set<Item> incompatibleUpgrades = new LinkedHashSet<>();

    protected UpgradeItem(Type type) {
        this.type = type;
        this.setCreativeTab(RegistrationHandler.CREATIVE_TAB);
    }

    public Type getType() {
        return type;
    }

    public void incompatibleWith(Item... upgrades) {
        incompatibleUpgrades.addAll(Arrays.asList(upgrades));
    }

    public Set<Item> getIncompatibleUpgrades(@Nonnull ItemStack stack) {
        return Collections.unmodifiableSet(incompatibleUpgrades);
    }

    public enum Type {
        STORAGE,
        UTILITY
    }
}
