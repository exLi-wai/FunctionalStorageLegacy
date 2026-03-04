package com.xinyihl.functionalstoragelgeacy.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DrawerItemBlock extends ItemBlock {

    public DrawerItemBlock(Block block) {
        super(block);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, java.util.List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        List<String> stored = collectStoredLines(stack);
        if (stored.isEmpty()) return;

        tooltip.add(TextFormatting.YELLOW + new TextComponentTranslation("drawer.tooltip.stored").getUnformattedText());
        for (String line : stored) {
            tooltip.add(TextFormatting.WHITE + line);
        }
    }

    private List<String> collectStoredLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("TileData")) {
            return lines;
        }

        NBTTagCompound tileData = stack.getTagCompound().getCompoundTag("TileData");

        if (tileData.hasKey("Inventory")) {
            NBTTagCompound inv = tileData.getCompoundTag("Inventory");
            if (inv.hasKey("BigItems")) {
                NBTTagCompound bigItems = inv.getCompoundTag("BigItems");
                for (String key : bigItems.getKeySet()) {
                    NBTTagCompound entry = bigItems.getCompoundTag(key);
                    NBTTagCompound stackTag = entry.getCompoundTag("Stack");
                    if (stackTag.getKeySet().isEmpty()) continue;
                    int amount = entry.getInteger("Amount");
                    if (amount <= 0) continue;
                    ItemStack item = new ItemStack(stackTag);
                    lines.add(item.getDisplayName() + "x" + amount);
                }
            }
        }

        if (tileData.hasKey("FluidInv")) {
            NBTTagCompound fluidInv = tileData.getCompoundTag("FluidInv");
            Set<String> keys = fluidInv.getKeySet();
            for (String key : keys) {
                if (!key.startsWith("Tank_")) continue;
                NBTTagCompound tankTag = fluidInv.getCompoundTag(key);
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(tankTag);
                if (fluid == null || fluid.amount <= 0) continue;
                lines.add(fluid.getLocalizedName() + "x" + formatCompact(fluid.amount));
            }
        }

        return lines;
    }

    private String formatCompact(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount % 1000 == 0) return (amount / 1000) + "k";
        return String.format("%.1fk", amount / 1000.0).replace(".0k", "k");
    }
}
