package com.xinyihl.functionalstoragelgeacy.block;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.StorageControllerTile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Abstract base class for all drawer blocks.
 * Handles facing, locked state, click interactions, and drop saving.
 */
public abstract class DrawerBlock extends Block {

    public static final PropertyEnum<Attachment> ATTACHMENT = PropertyEnum.create("attachment", Attachment.class);
    public static final PropertyDirection HORIZONTAL_FACING = PropertyDirection.create("horizontal_facing", EnumFacing.Plane.HORIZONTAL);
    private static final double DEFAULT_REACH_DISTANCE = 5.0D;
    private static final double HIT_EPSILON = 1.0E-3D;
    private static final EnumMap<HitBoxLayout, List<AxisAlignedBB>> BASE_HIT_BOXES = new EnumMap<>(HitBoxLayout.class);
    private static final EnumMap<HitBoxLayout, EnumMap<Attachment, EnumMap<EnumFacing, List<AxisAlignedBB>>>> CACHED_HIT_BOXES = new EnumMap<>(HitBoxLayout.class);
    private static final EnumFacing[] HORIZONTAL_VALUES = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    static {
        BASE_HIT_BOXES.put(HitBoxLayout.X_1, Collections.singletonList(
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D)
        ));
        BASE_HIT_BOXES.put(HitBoxLayout.X_2, Arrays.asList(
                new AxisAlignedBB(1 / 16D, 9 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D),
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 15 / 16D, 7 / 16D, 1 / 16D)
        ));
        BASE_HIT_BOXES.put(HitBoxLayout.X_3, Arrays.asList(
                new AxisAlignedBB(1 / 16D, 9 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D),
                new AxisAlignedBB(9 / 16D, 1 / 16D, 0, 15 / 16D, 7 / 16D, 1 / 16D),
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 7 / 16D, 7 / 16D, 1 / 16D)
        ));
        BASE_HIT_BOXES.put(HitBoxLayout.X_4, Arrays.asList(
                new AxisAlignedBB(9 / 16D, 9 / 16D, 0, 15 / 16D, 15 / 16D, 1 / 16D),
                new AxisAlignedBB(1 / 16D, 9 / 16D, 0, 7 / 16D, 15 / 16D, 1 / 16D),
                new AxisAlignedBB(9 / 16D, 1 / 16D, 0, 15 / 16D, 7 / 16D, 1 / 16D),
                new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 7 / 16D, 7 / 16D, 1 / 16D)
        ));
    }

    public DrawerBlock(Material material) {
        super(material);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(ATTACHMENT, Attachment.WALL)
                .withProperty(HORIZONTAL_FACING, EnumFacing.NORTH));
        this.setHardness(2.5F);
        this.setResistance(8.0F);

        this.useNeighborBrightness = true;
        this.setLightOpacity(255);

        this.setCreativeTab(FunctionalStorageLgeacy.CREATIVE_TAB);
    }

    public static Attachment getAttachment(IBlockState state) {
        return state.getValue(ATTACHMENT);
    }

    public static EnumFacing getHorizontalFacing(IBlockState state) {
        return state.getValue(HORIZONTAL_FACING);
    }

    public static EnumFacing getFrontFacing(IBlockState state) {
        Attachment attachment = getAttachment(state);
        if (attachment == Attachment.FLOOR) {
            return EnumFacing.UP;
        }
        if (attachment == Attachment.CEILING) {
            return EnumFacing.DOWN;
        }
        return getHorizontalFacing(state);
    }

    public static int getHorizontalFacingIndex(EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return 0;
            case EAST:
                return 1;
            case SOUTH:
                return 2;
            case WEST:
            default:
                return 3;
        }
    }

    private static EnumFacing getPlacementFacingFromRay(@Nullable EntityLivingBase placer) {
        if (placer == null) {
            return EnumFacing.NORTH;
        }
        Vec3d lookVec = placer.getLookVec();
        return EnumFacing.getFacingFromVector((float) -lookVec.x, (float) -lookVec.y, (float) -lookVec.z);
    }

    private static Attachment getAttachmentForPlacement(EnumFacing placementFacing) {
        if (placementFacing == EnumFacing.UP) {
            return Attachment.FLOOR;
        }
        if (placementFacing == EnumFacing.DOWN) {
            return Attachment.CEILING;
        }
        return Attachment.WALL;
    }

    private static EnumFacing getHorizontalFacingForPlacement(Attachment attachment, EnumFacing placementFacing, @Nullable EntityLivingBase placer) {
        if (attachment == Attachment.WALL && placementFacing.getAxis().isHorizontal()) {
            return placementFacing;
        }
        EnumFacing playerHorizontalFacing = placer != null ? placer.getHorizontalFacing() : EnumFacing.NORTH;
        if (attachment == Attachment.FLOOR) {
            return playerHorizontalFacing.getOpposite();
        }
        if (attachment == Attachment.CEILING) {
            return playerHorizontalFacing.getOpposite();
        }
        return playerHorizontalFacing;
    }

    private static double getPlayerReachDistance(EntityPlayer player) {
        if (player == null) {
            return DEFAULT_REACH_DISTANCE;
        }
        if (player.getEntityAttribute(EntityPlayer.REACH_DISTANCE) != null) {
            return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        }
        return DEFAULT_REACH_DISTANCE;
    }

    private static boolean containsHitVec(AxisAlignedBB hitBox, Vec3d localHitVec) {
        return localHitVec.x >= hitBox.minX - HIT_EPSILON && localHitVec.x <= hitBox.maxX + HIT_EPSILON
                && localHitVec.y >= hitBox.minY - HIT_EPSILON && localHitVec.y <= hitBox.maxY + HIT_EPSILON
                && localHitVec.z >= hitBox.minZ - HIT_EPSILON && localHitVec.z <= hitBox.maxZ + HIT_EPSILON;
    }

    private static List<AxisAlignedBB> buildHitBoxes(HitBoxLayout layout, Attachment attachment, EnumFacing horizontalFacing) {
        List<AxisAlignedBB> baseHitBoxes = BASE_HIT_BOXES.get(layout);
        if (baseHitBoxes == null || baseHitBoxes.isEmpty()) {
            return Collections.emptyList();
        }

        EnumFacing frontFacing = getFrontFacingForPlacement(attachment, horizontalFacing);
        EnumFacing upDirection = getUpDirectionForFace(attachment, horizontalFacing);
        EnumFacing leftDirection = getLeftDirection(frontFacing, upDirection);
        EnumFacing inwardDirection = frontFacing.getOpposite();

        List<AxisAlignedBB> transformedHitBoxes = new ArrayList<>(baseHitBoxes.size());
        for (AxisAlignedBB baseHitBox : baseHitBoxes) {
            transformedHitBoxes.add(transformHitBox(baseHitBox, leftDirection, upDirection, inwardDirection));
        }
        return Collections.unmodifiableList(transformedHitBoxes);
    }

    private static EnumFacing getFrontFacingForPlacement(Attachment attachment, EnumFacing horizontalFacing) {
        if (attachment == Attachment.FLOOR) {
            return EnumFacing.UP;
        }
        if (attachment == Attachment.CEILING) {
            return EnumFacing.DOWN;
        }
        return horizontalFacing;
    }

    private static EnumFacing getUpDirectionForFace(Attachment attachment, EnumFacing horizontalFacing) {
        if (attachment == Attachment.FLOOR) {
            return horizontalFacing.getOpposite();
        }
        if (attachment == Attachment.CEILING) {
            return horizontalFacing;
        }
        return EnumFacing.UP;
    }

    private static EnumFacing getLeftDirection(EnumFacing frontFacing, EnumFacing upDirection) {
        int crossX = frontFacing.getYOffset() * upDirection.getZOffset() - frontFacing.getZOffset() * upDirection.getYOffset();
        int crossY = frontFacing.getZOffset() * upDirection.getXOffset() - frontFacing.getXOffset() * upDirection.getZOffset();
        int crossZ = frontFacing.getXOffset() * upDirection.getYOffset() - frontFacing.getYOffset() * upDirection.getXOffset();
        return EnumFacing.getFacingFromVector(crossX, crossY, crossZ);
    }

    private static AxisAlignedBB transformHitBox(AxisAlignedBB localHitBox, EnumFacing leftDirection, EnumFacing upDirection, EnumFacing inwardDirection) {
        Vec3d[] corners = new Vec3d[]{
                transformPoint(localHitBox.minX, localHitBox.minY, localHitBox.minZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.minX, localHitBox.minY, localHitBox.maxZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.minX, localHitBox.maxY, localHitBox.minZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.minX, localHitBox.maxY, localHitBox.maxZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.maxX, localHitBox.minY, localHitBox.minZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.maxX, localHitBox.minY, localHitBox.maxZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.maxX, localHitBox.maxY, localHitBox.minZ, leftDirection, upDirection, inwardDirection),
                transformPoint(localHitBox.maxX, localHitBox.maxY, localHitBox.maxZ, leftDirection, upDirection, inwardDirection)
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3d corner : corners) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Vec3d transformPoint(double localX, double localY, double localZ, EnumFacing leftDirection, EnumFacing upDirection, EnumFacing inwardDirection) {
        double originX = leftDirection.getXOffset() < 0 || upDirection.getXOffset() < 0 || inwardDirection.getXOffset() < 0 ? 1.0D : 0.0D;
        double originY = leftDirection.getYOffset() < 0 || upDirection.getYOffset() < 0 || inwardDirection.getYOffset() < 0 ? 1.0D : 0.0D;
        double originZ = leftDirection.getZOffset() < 0 || upDirection.getZOffset() < 0 || inwardDirection.getZOffset() < 0 ? 1.0D : 0.0D;

        return new Vec3d(
                originX + localX * leftDirection.getXOffset() + localY * upDirection.getXOffset() + localZ * inwardDirection.getXOffset(),
                originY + localX * leftDirection.getYOffset() + localY * upDirection.getYOffset() + localZ * inwardDirection.getYOffset(),
                originZ + localX * leftDirection.getZOffset() + localY * upDirection.getZOffset() + localZ * inwardDirection.getZOffset()
        );
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
    public boolean hasTileEntity(@Nonnull IBlockState state) {
        return true;
    }

    @Override
    public void onBlockClicked(World worldIn, @Nonnull BlockPos pos, @Nonnull EntityPlayer playerIn) {
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
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            copyFromStack(stack, drawerTile);
        }
    }

    @Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            drops.add(createStackWithTileData((ControllableDrawerTile) te));
        }
    }

    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
        if (willHarvest) return true; // Delay removal for getDrops
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(@Nonnull World worldIn, @Nonnull EntityPlayer player, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable TileEntity te, @Nonnull ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    @Override
    public void breakBlock(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
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

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            return createStackWithTileData((ControllableDrawerTile) te);
        }
        return new ItemStack(this);
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    public boolean canProvidePower(@Nonnull IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(@Nonnull IBlockState state, IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            return ((ControllableDrawerTile) te).getRedstoneSignal(side);
        }
        return 0;
    }

    /**
     * Get the drawer type for this block. Returns null if not applicable.
     */
    public DrawerType getDrawerType() {
        return null;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ATTACHMENT, HORIZONTAL_FACING);
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        int safeMeta = Math.max(0, Math.min(meta, 11));
        Attachment attachment = Attachment.byIndex(safeMeta / 4);
        EnumFacing horizontalFacing = HORIZONTAL_VALUES[safeMeta % 4];
        return this.getDefaultState()
                .withProperty(ATTACHMENT, attachment)
                .withProperty(HORIZONTAL_FACING, horizontalFacing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ATTACHMENT).getIndex() * 4 + getHorizontalFacingIndex(state.getValue(HORIZONTAL_FACING));
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, @Nonnull EnumHand hand) {
        EnumFacing placementFacing = getPlacementFacingFromRay(placer);
        Attachment attachment = getAttachmentForPlacement(placementFacing);
        EnumFacing horizontalFacing = getHorizontalFacingForPlacement(attachment, placementFacing, placer);
        return this.getDefaultState()
                .withProperty(ATTACHMENT, attachment)
                .withProperty(HORIZONTAL_FACING, horizontalFacing);
    }

    @Override
    public boolean onBlockActivated(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawerTile = (ControllableDrawerTile) te;
            int slot = getHitSlot(state, worldIn, pos, playerIn);
            return drawerTile.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot);
        }
        return false;
    }

    /**
     * Create an ItemStack with tile entity data saved.
     */
    public ItemStack createStackWithTileData(ControllableDrawerTile tile) {
        ItemStack stack = new ItemStack(this);
        if (tile.isLocked() || !tile.isEverythingEmpty()) {
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
     * Determine which slot was clicked based on ray trace hit position on the front face.
     * Returns -1 if no specific slot was hit (e.g. clicked on the side).
     */
    public int getHitSlot(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        RayTraceResult hitResult = rayTraceFrontFace(state, world, pos, player);
        if (hitResult == null || hitResult.hitVec == null) {
            return -1;
        }
        Vec3d localHitVec = hitResult.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
        int slotIndex = 0;
        for (AxisAlignedBB hitBox : getHitBoxes(state)) {
            if (containsHitVec(hitBox, localHitVec)) {
                return slotIndex;
            }
            slotIndex++;
        }
        return -1;
    }

    /**
     * Get the visual hit shapes for slot detection.
     */
    public Collection<AxisAlignedBB> getHitBoxes(IBlockState state) {
        HitBoxLayout layout = getHitBoxLayout();
        if (layout == null) {
            return Collections.emptyList();
        }
        Attachment attachment = getAttachment(state);
        EnumFacing horizontalFacing = getHorizontalFacing(state);
        EnumMap<Attachment, EnumMap<EnumFacing, List<AxisAlignedBB>>> attachmentCache = CACHED_HIT_BOXES.get(layout);
        if (attachmentCache == null) {
            attachmentCache = new EnumMap<>(Attachment.class);
            CACHED_HIT_BOXES.put(layout, attachmentCache);
        }
        EnumMap<EnumFacing, List<AxisAlignedBB>> facingCache = attachmentCache.get(attachment);
        if (facingCache == null) {
            facingCache = new EnumMap<>(EnumFacing.class);
            attachmentCache.put(attachment, facingCache);
        }
        List<AxisAlignedBB> cachedShapes = facingCache.get(horizontalFacing);
        if (cachedShapes == null) {
            cachedShapes = buildHitBoxes(layout, attachment, horizontalFacing);
            facingCache.put(horizontalFacing, cachedShapes);
        }
        return cachedShapes;
    }

    @Nullable
    protected HitBoxLayout getHitBoxLayout() {
        return null;
    }

    @Nullable
    protected RayTraceResult rayTraceFrontFace(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double reach = getPlayerReachDistance(player);
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);
        RayTraceResult hitResult = this.collisionRayTrace(state, world, pos, start, end);
        if (hitResult == null || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return null;
        }
        return hitResult.sideHit == getFrontFacing(state) ? hitResult : null;
    }

    public enum Attachment implements IStringSerializable {
        WALL("wall", 0),
        FLOOR("floor", 1),
        CEILING("ceiling", 2);

        private final String name;
        private final int index;

        Attachment(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public static Attachment byIndex(int index) {
            for (Attachment attachment : values()) {
                if (attachment.index == index) {
                    return attachment;
                }
            }
            return WALL;
        }

        public int getIndex() {
            return index;
        }

        @Nonnull
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected enum HitBoxLayout {
        X_1,
        X_2,
        X_3,
        X_4
    }
}
