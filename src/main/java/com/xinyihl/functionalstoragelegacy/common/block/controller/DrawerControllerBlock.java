package com.xinyihl.functionalstoragelegacy.common.block.controller;

import com.xinyihl.functionalstoragelegacy.common.block.base.DrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block for the drawer controller.
 * Aggregates connected drawers into a unified item/fluid handler.
 */
public class DrawerControllerBlock extends DrawerBlock {

    public DrawerControllerBlock() {
        super(Material.IRON);
        this.setRegistryName("storage_controller");
        this.setTranslationKey("functionalstoragelegacy.storage_controller");
        this.setHardness(5.0F);
        this.setResistance(10.0F);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new DrawerControllerTile();
    }

    @Override
    public ItemStack createStackWithTileData(ControllableDrawerTile tile) {
        return new ItemStack(this);
    }

    @Override
    public void breakBlock(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof DrawerControllerTile) {
            ItemStackHandler inv = ((DrawerControllerTile) te).getStorageUpgrades();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    spawnAsEntity(worldIn, pos, stack);
                    inv.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }
}
