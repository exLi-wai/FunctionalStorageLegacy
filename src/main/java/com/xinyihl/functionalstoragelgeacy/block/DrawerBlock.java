package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.StorageControllerTile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Abstract base class for all drawer blocks.
 * Handles facing, locked state, click interactions, and drop saving.
 */
public abstract class DrawerBlock extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool LOCKED = PropertyBool.create("locked");

    public DrawerBlock(Material material) {
        super(material);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LOCKED, false));
        this.setHardness(2.5F);
        this.setResistance(8.0F);

        this.useNeighborBrightness = true;
        this.setLightOpacity(255);

        this.setCreativeTab(FunctionalStorageLgeacy.CREATIVE_TAB);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LOCKED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
        boolean locked = (meta & 4) != 0;
        return this.getDefaultState().withProperty(FACING, facing).withProperty(LOCKED, locked);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(LOCKED)) meta |= 4;
        return meta;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            int slot = getHitSlot(state, worldIn, pos, playerIn);
            return drawerTile.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot);
        }
        return false;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (worldIn.isRemote) return;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            int slot = getHitSlot(worldIn.getBlockState(pos), worldIn, pos, playerIn);
            if (slot != -1) {
                drawerTile.onClicked(playerIn, slot);
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            copyFromStack(stack, drawerTile);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            drops.add(createStackWithTileData((ControllableDrawerTile) te));
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) return true; // Delay removal for getDrops
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    /**
     * Create an ItemStack with tile entity data saved.
     */
    public ItemStack createStackWithTileData(ControllableDrawerTile tile) {
        ItemStack stack = new ItemStack(this);
        if (!tile.isEverythingEmpty()) {
            NBTTagCompound tileTag = tile.saveTileToNBT();
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setTag("TileData", tileTag);
        }
        if (tile.isLocked()) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setBoolean("Locked", true);
        }
        return stack;
    }

    /**
     * Copy data from an ItemStack to a placed tile entity.
     */
    protected void copyFromStack(ItemStack stack, ControllableDrawerTile tile) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("TileData")) {
                tile.loadTileFromNBT(tag.getCompoundTag("TileData"));
            }
            if (tag.hasKey("Locked")) {
                tile.setLocked(tag.getBoolean("Locked"));
            }
            tile.sendUpdatePacket();
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            if (drawerTile.getControllerPos() != null) {
                TileEntity controllerTE = worldIn.getTileEntity(drawerTile.getControllerPos());
                if (controllerTE instanceof StorageControllerTile) {
                    ((StorageControllerTile) controllerTE).removeConnectedDrawer(pos);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            return createStackWithTileData((ControllableDrawerTile) te);
        }
        return new ItemStack(this);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            return ((ControllableDrawerTile) te).getRedstoneSignal(side);
        }
        return 0;
    }

    /**
     * Determine which slot was clicked based on ray trace hit position on the front face.
     * Returns -1 if no specific slot was hit (e.g. clicked on the side).
     */
    public abstract int getHitSlot(IBlockState state, World world, BlockPos pos, EntityPlayer player);

    /**
     * Get the visual hit shapes for slot detection.
     */
    public abstract Collection<AxisAlignedBB> getHitBoxes(IBlockState state);

    /**
     * Get the drawer type for this block. Returns null if not applicable.
     */
    public DrawerType getDrawerType() {
        return null;
    }
}
