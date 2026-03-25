package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

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

    public boolean isMaxStorageUpgrade() {
        return tier == StorageTier.MAX;
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
            tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("item.functionalstoragelegacy.storage_upgrade.multiplier", TextFormatting.WHITE + "" + tier.getMultiplier() + "x").getUnformattedText());
        }
    }

    /**
     * Storage upgrade tiers with their capacity multipliers.
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

        public float getMultiplier() {
            switch (this) {
                case COPPER:
                    return Configurations.STORAGE.copperMultiplier;
                case GOLD:
                    return Configurations.STORAGE.goldMultiplier;
                case DIAMOND:
                    return Configurations.STORAGE.diamondMultiplier;
                case NETHERITE:
                    return Configurations.STORAGE.netheriteMultiplier;
                case MAX:
                    return 1.0f;
                case IRON:
                default:
                    return 1.0f;
            }
        }

        public boolean isHigherThan(StorageTier other) {
            return ordinal() > other.ordinal();
        }

        public String getName() {
            return name;
        }
    }
}
