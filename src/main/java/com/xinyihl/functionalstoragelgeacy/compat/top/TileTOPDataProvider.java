package com.xinyihl.functionalstoragelgeacy.compat.top;

import com.xinyihl.functionalstoragelgeacy.Tags;
import com.xinyihl.functionalstoragelgeacy.block.tile.CompactingDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.DrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.EnderDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelgeacy.inventory.BigInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.CompactingInventoryHandler;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class TileTOPDataProvider implements IProbeInfoProvider {
    public TileTOPDataProvider() {
    }

    @Override
    public String getID() {
        return Tags.MOD_ID + ":" + this.getClass().getSimpleName();
    }

    protected String i18n(String key) {
        return "{*tooltip.functionalstoragelgeacy." + key + "*}";
    }

    protected String formatCompact(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount % 1000 == 0) return (amount / 1000) + "k";
        return String.format("%.1fk", amount / 1000.0).replace(".0k", "k");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        TileEntity te = world.getTileEntity(data.getPos());
        if (!(te instanceof ControllableDrawerTile)) {
            return;
        }

        List<String> storedLines = new ArrayList<>();

        if (te instanceof DrawerTile) {
            BigInventoryHandler handler = ((DrawerTile) te).getHandler();
            for (int i = 0; i < handler.getSlotCount(); i++) {
                BigInventoryHandler.BigStack big = handler.getBigStack(i);
                if (big.getStack().isEmpty() || big.getAmount() <= 0) continue;
                storedLines.add(big.getStack().getDisplayName() + "x" + big.getAmount());
            }
        } else if (te instanceof EnderDrawerTile) {
            EnderDrawerTile drawer = (EnderDrawerTile) te;
            if (drawer.getItemHandler() != null) {
                for (int i = 0; i < drawer.getItemHandler().getSlots(); i++) {
                    ItemStack stack = drawer.getItemHandler().getStackInSlot(i);
                    if (stack.isEmpty() || stack.getCount() <= 0) continue;
                    storedLines.add(stack.getDisplayName() + "x" + stack.getCount());
                }
            }
        } else if (te instanceof CompactingDrawerTile) {
            CompactingInventoryHandler handler = ((CompactingDrawerTile) te).getHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty() || stack.getCount() <= 0) continue;
                storedLines.add(stack.getDisplayName() + "x" + stack.getCount());
            }
        }

        if (te instanceof FluidDrawerTile) {
            FluidDrawerTile fluidDrawer = (FluidDrawerTile) te;
            for (int i = 0; i < fluidDrawer.getFluidHandler().getTanksCount(); i++) {
                FluidStack fluid = fluidDrawer.getFluidHandler().getTankFluid(i);
                if (fluid == null || fluid.amount <= 0) continue;
                storedLines.add(fluid.getLocalizedName() + "x" + formatCompact(fluid.amount));
            }
        }

        if (!storedLines.isEmpty()) {
            probeInfo.text(i18n("stored"));
            for (String line : storedLines) {
                probeInfo.text(line);
            }
        }

    }
}