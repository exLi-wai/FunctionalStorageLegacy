package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.block.tile.CompactingDrawerTile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block for the compacting drawer (3-slot nugget/ingot/block auto-compressing).
 */
public class CompactingDrawerBlock extends DrawerBlock {

    public CompactingDrawerBlock() {
        super(Material.ROCK);
        this.setRegistryName("compacting_drawer");
        this.setTranslationKey("functionalstoragelgeacy.compacting_drawer");
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new CompactingDrawerTile();
    }

    @Override
    protected HitBoxLayout getHitBoxLayout() {
        return HitBoxLayout.X_3;
    }
}
