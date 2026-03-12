package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

import com.xinyihl.functionalstoragelegacy.FunctionalStorageLegacy;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.misc.FunctionalStorageConfig;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;

public class StoneGenerationUpgradeItem extends UtilityUpgradeItem {

    private final StoneTier tier;
    private static final String TIMER_KEY = "StoneGenTimer";

    public StoneGenerationUpgradeItem(StoneTier tier) {
        super(UtilityAction.NONE);
        this.setCreativeTab(FunctionalStorageLegacy.CREATIVE_TAB);
        this.setMaxStackSize(1);
        this.tier = tier;
    }

    @Override
    public void onTick(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot) {
        if (tile.getWorld().isRemote) {
            return;
        }

        if (!upgradeStack.hasTagCompound()) {
            upgradeStack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = upgradeStack.getTagCompound();

        if (!nbt.hasKey(TIMER_KEY)) {
            nbt.setInteger(TIMER_KEY, getGenerationInterval());
        }

        int timer = nbt.getInteger(TIMER_KEY);
        timer--;
        nbt.setInteger(TIMER_KEY, timer);

        if (timer <= 0) {
            if (generateAndStoreStone(tile)) {
                nbt.setInteger(TIMER_KEY, getGenerationInterval());
            } else {
                nbt.setInteger(TIMER_KEY, 100);
            }
        }
    }

    private int getGenerationInterval() {
        return (int) Math.ceil(20.0 / tier.getGenerationRate());
    }

    private boolean generateAndStoreStone(ControllableDrawerTile tile) {
        IItemHandler handler = tile.getItemHandler();
        if (handler == null) {
            return false;
        }

        ItemStack stoneStack = new ItemStack(Blocks.COBBLESTONE, 1);

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stoneStack, false);

        return remainder.isEmpty() || remainder.getCount() < stoneStack.getCount();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        String rateText = String.format("%.1f/s", tier.getGenerationRate());
        tooltip.add(TextFormatting.YELLOW + I18n.format("item.functionalstoragelegacy.generation_upgrade.rate") + TextFormatting.WHITE + rateText);

    }

    public enum StoneTier {
        BASIC(FunctionalStorageConfig.STONE_GENERATION_T1, 1),
        ADVANCED(FunctionalStorageConfig.STONE_GENERATION_T2, 2),
        REINFORCED(FunctionalStorageConfig.STONE_GENERATION_T3, 3),
        MAGICAL(FunctionalStorageConfig.STONE_GENERATION_T4, 4);

        private final float generationRate;
        private final int tier;

        StoneTier(float generationRate, int tier) {
            this.generationRate = generationRate;
            this.tier = tier;
        }

        public float getGenerationRate() {
            return generationRate;
        }

        public int getTier() {
            return tier;
        }
    }
}