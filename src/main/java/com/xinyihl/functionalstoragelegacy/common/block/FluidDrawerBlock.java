package com.xinyihl.functionalstoragelegacy.common.block;

import com.xinyihl.functionalstoragelegacy.api.DrawerType;
import com.xinyihl.functionalstoragelegacy.common.block.base.DrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.tile.FluidDrawerTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block for fluid drawers. Supports X_1, X_2, X_4 fluid slot configurations.
 */
public class FluidDrawerBlock extends DrawerBlock {

    private final DrawerType drawerType;

    public FluidDrawerBlock(DrawerType drawerType) {
        super(Material.ROCK);
        this.drawerType = drawerType;
        this.setRegistryName("fluid_" + drawerType.getSlots());
        this.setTranslationKey("functionalstoragelegacy.fluid_" + drawerType.getSlots());
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new FluidDrawerTile(drawerType);
    }

    @Override
    protected HitBoxLayout getHitBoxLayout() {
        switch (drawerType) {
            case X_1:
                return HitBoxLayout.X_1;
            case X_2:
                return HitBoxLayout.X_2;
            case X_4:
                return HitBoxLayout.X_4;
            default:
                return null;
        }
    }

    public DrawerType getDrawerType() {
        return drawerType;
    }

    @Nonnull
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
