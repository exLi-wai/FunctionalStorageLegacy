package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

import com.xinyihl.functionalstoragelegacy.misc.FunctionalStorageConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Item for storage upgrades that increase drawer capacity.
 * Has different tiers (IRON/COPPER/GOLD/DIAMOND/NETHERITE).
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

    @Override
    public boolean hasEffect(@Nonnull ItemStack stack) {
        return tier == StorageTier.NETHERITE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (tier == StorageTier.IRON) {
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("item.functionalstoragelegacy.iron_downgrade.desc").getUnformattedText());
        } else {
            tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelegacy.storage_upgrade.multiplier", TextFormatting.WHITE + "" + tier.getMultiplier() + "x").getUnformattedText());
        }
    }

    /**
     * Storage upgrade tiers with their capacity multipliers.
     */
    public enum StorageTier {
        IRON(1, "iron"),
        COPPER(FunctionalStorageConfig.COPPER_MULTIPLIER, "copper"),
        GOLD(FunctionalStorageConfig.GOLD_MULTIPLIER, "gold"),
        DIAMOND(FunctionalStorageConfig.DIAMOND_MULTIPLIER, "diamond"),
        NETHERITE(FunctionalStorageConfig.NETHERITE_MULTIPLIER, "netherite");

        private final float multiplier;
        private final String name;

        StorageTier(float multiplier, String name) {
            this.multiplier = multiplier;
            this.name = name;
        }

        public float getMultiplier() {
            return multiplier;
        }

        public String getName() {
            return name;
        }
    }
}
