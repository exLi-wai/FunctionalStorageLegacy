package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.block.tile.DrawerTile;
import com.xinyihl.functionalstoragelgeacy.util.DrawerWoodType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Standard wooden drawer block.
 * Each variant is defined by a wood type and drawer type (1/2/4 slots).
 */
public class WoodDrawerBlock extends DrawerBlock {

    private final DrawerType drawerType;
    private final DrawerWoodType woodType;

    public WoodDrawerBlock(DrawerWoodType woodType, DrawerType drawerType) {
        super(Material.WOOD);
        this.woodType = woodType;
        this.drawerType = drawerType;
        this.setRegistryName(woodType.getName() + "_" + drawerType.getSlots());
        this.setTranslationKey("functionalstoragelgeacy." + woodType.getName() + "_" + drawerType.getSlots());
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new DrawerTile(drawerType, woodType);
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
}
