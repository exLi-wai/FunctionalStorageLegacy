package com.xinyihl.functionalstoragelegacy.misc;

import com.xinyihl.functionalstoragelegacy.client.gui.GuiDrawer;
import com.xinyihl.functionalstoragelegacy.common.container.ContainerDrawer;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {

    public static final int GUI_DRAWER = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (ID) {
            case GUI_DRAWER:
                if (te instanceof ControllableDrawerTile) {
                    return new ContainerDrawer(player.inventory, (ControllableDrawerTile) te);
                }
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (ID) {
            case GUI_DRAWER:
                if (te instanceof ControllableDrawerTile) {
                    return new GuiDrawer(new ContainerDrawer(player.inventory, (ControllableDrawerTile) te));
                }
                break;
        }
        return null;
    }
}
