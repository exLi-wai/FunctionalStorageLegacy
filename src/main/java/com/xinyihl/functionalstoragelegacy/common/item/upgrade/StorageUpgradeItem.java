package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

import com.xinyihl.functionalstoragelegacy.api.upgrade.ModifierType;
import com.xinyihl.functionalstoragelegacy.api.upgrade.UpgradeModifier;
import com.xinyihl.functionalstoragelegacy.misc.Configurations;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Item for storage upgrades that increase drawer capacity.
 * Has different tiers (IRON/COPPER/GOLD/DIAMOND/NETHERITE/MAX).
 * Each tier carries a set of {@link UpgradeModifier}s keyed by {@link ModifierType}.
 */
public class StorageUpgradeItem extends UpgradeItem {

    private final StorageTier tier;

    public StorageUpgradeItem(StorageTier tier) {
        super(Type.STORAGE);
        this.tier = tier;
    }

    public StorageTier getTier() {
        return tier;
    }

    public boolean isMaxStorageUpgrade() {
        return tier == StorageTier.MAX;
    }

    public Map<ModifierType, UpgradeModifier> getModifiers() {
        switch (tier) {
            case IRON: {
                Map<ModifierType, UpgradeModifier> map = new EnumMap<>(ModifierType.class);
                map.put(ModifierType.ITEM_STORAGE, new UpgradeModifier.SetBase(1));
                map.put(ModifierType.FLUID_STORAGE, new UpgradeModifier.SetBase(1));
                return map;
            }
            case COPPER:
            case GOLD:
            case DIAMOND:
            case NETHERITE: {
                float mult = getItemStorageMultiplier(tier);
                Map<ModifierType, UpgradeModifier> map = new EnumMap<>(ModifierType.class);
                map.put(ModifierType.ITEM_STORAGE, new UpgradeModifier.MultiplyFactor(mult));
                map.put(ModifierType.FLUID_STORAGE, new UpgradeModifier.MultiplyFactor(mult / Configurations.STORAGE.fluidDivisor));
                map.put(ModifierType.CONTROLLER_RANGE, new UpgradeModifier.AddToBase(mult / Configurations.STORAGE.rangeDivisor));
                return map;
            }
            case MAX:
            default:
                return Collections.emptyMap();
        }
    }


    public float getItemStorageMultiplier(StorageUpgradeItem.StorageTier tier) {
        switch (tier) {
            case COPPER:
                return Configurations.STORAGE.copperMultiplier;
            case GOLD:
                return Configurations.STORAGE.goldMultiplier;
            case DIAMOND:
                return Configurations.STORAGE.diamondMultiplier;
            case NETHERITE:
                return Configurations.STORAGE.netheriteMultiplier;
            case IRON:
            case MAX:
            default:
                return 1.0f;
        }
    }

    @Override
    public boolean hasEffect(@Nonnull ItemStack stack) {
        return tier == StorageTier.NETHERITE || tier == StorageTier.MAX;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (tier == StorageTier.IRON) {
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("item.functionalstoragelegacy.iron_downgrade.desc").getUnformattedText());
        } else if (tier == StorageTier.MAX) {
            tooltip.add(TextFormatting.GOLD + new TextComponentTranslation("item.functionalstoragelegacy.max_storage_upgrade.desc").getUnformattedText());
        } else {
            tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelegacy.storage_upgrade.multiplier", TextFormatting.WHITE + "" + getItemStorageMultiplier(tier) + "x").getUnformattedText());
        }
    }

    /**
     * Storage upgrade tiers with their capacity modifiers.
     * Each tier provides a map of {@link ModifierType} -> {@link UpgradeModifier}
     * describing how the tier affects different aspects of a drawer.
     */
    public enum StorageTier {
        IRON("iron"),
        COPPER("copper"),
        GOLD("gold"),
        DIAMOND("diamond"),
        NETHERITE("netherite"),
        MAX("max");

        private final String name;

        StorageTier(String name) {
            this.name = name;
        }

        public boolean isHigherThan(StorageTier other) {
            return ordinal() > other.ordinal();
        }

        public String getName() {
            return name;
        }
    }
}
