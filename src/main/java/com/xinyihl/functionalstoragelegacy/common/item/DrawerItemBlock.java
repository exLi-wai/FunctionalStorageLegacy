package com.xinyihl.functionalstoragelegacy.common.item;

import com.xinyihl.functionalstoragelegacy.api.DrawerType;
import com.xinyihl.functionalstoragelegacy.common.block.FluidDrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.block.WoodDrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.block.compact.CompactingDrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.block.compact.SimpleCompactingDrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.inventory.capability.CompactingStackItemHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.capability.DrawerStackCapabilityProvider;
import com.xinyihl.functionalstoragelegacy.common.inventory.capability.DrawerStackItemHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.capability.FluidDrawerStackItemHandler;
import com.xinyihl.functionalstoragelegacy.util.NumberUtils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DrawerItemBlock extends ItemBlock {

    public DrawerItemBlock(Block block) {
        super(block);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
        IItemHandler itemHandler = createItemHandler(stack);
        IFluidHandlerItem fluidHandler = createFluidItemHandler(stack);
        if (itemHandler == null && fluidHandler == null) {
            return null;
        }
        return new DrawerStackCapabilityProvider(itemHandler, fluidHandler);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull java.util.List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
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

        if (stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stored = handler.getStackInSlot(slot);
                    if (!stored.isEmpty() && stored.getCount() > 0) {
                        lines.add(stored.getDisplayName() + "x" + NumberUtils.formatCompact(stored.getCount()));
                    }
                }
            }
            if (!lines.isEmpty()) {
                return lines;
            }
        }

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
                    long amount = entry.getLong("Amount");
                    if (amount <= 0) continue;
                    ItemStack item = new ItemStack(stackTag);
                    lines.add(item.getDisplayName() + "x" + NumberUtils.formatCompact(amount));
                }
            }
        }

        if (tileData.hasKey("CompactingInv")) {
            NBTTagCompound compactingInv = tileData.getCompoundTag("CompactingInv");
            long totalBase = compactingInv.getLong("TotalBase");
            int slotCount = tileData.hasKey("SlotCount") ? tileData.getInteger("SlotCount") : 3;
            for (int i = 0; i < slotCount; i++) {
                String key = "Result_" + i;
                if (!compactingInv.hasKey(key)) continue;
                NBTTagCompound entry = compactingInv.getCompoundTag(key);
                NBTTagCompound stackTag = entry.getCompoundTag("Stack");
                if (stackTag.getKeySet().isEmpty()) continue;
                int needed = Math.max(1, entry.getInteger("Needed"));
                long amount = totalBase / needed;
                if (amount <= 0) continue;
                ItemStack item = new ItemStack(stackTag);
                lines.add(item.getDisplayName() + "x" + NumberUtils.formatCompact(amount));
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
                lines.add(fluid.getLocalizedName() + "x" + NumberUtils.formatCompact(fluid.amount));
            }
        }

        return lines;
    }

    @Nullable
    private IItemHandler createItemHandler(ItemStack stack) {
        if (block instanceof WoodDrawerBlock) {
            DrawerType drawerType = ((WoodDrawerBlock) block).getDrawerType();
            return new DrawerStackItemHandler(stack, drawerType);
        }
        if (block instanceof CompactingDrawerBlock) {
            return new CompactingStackItemHandler(stack, 3);
        }
        if (block instanceof SimpleCompactingDrawerBlock) {
            return new CompactingStackItemHandler(stack, 2);
        }
        return null;
    }

    @Nullable
    private IFluidHandlerItem createFluidItemHandler(ItemStack stack) {
        if (block instanceof FluidDrawerBlock) {
            DrawerType drawerType = ((FluidDrawerBlock) block).getDrawerType();
            return new FluidDrawerStackItemHandler(stack, drawerType);
        }
        return null;
    }
}
