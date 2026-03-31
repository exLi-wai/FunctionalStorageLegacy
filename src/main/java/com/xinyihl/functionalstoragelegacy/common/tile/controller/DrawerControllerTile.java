package com.xinyihl.functionalstoragelegacy.common.tile.controller;

import com.xinyihl.functionalstoragelegacy.FunctionalStorageLegacy;
import com.xinyihl.functionalstoragelegacy.api.ILockable;
import com.xinyihl.functionalstoragelegacy.client.render.DrawerOptions;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerItemHandler;
import com.xinyihl.functionalstoragelegacy.common.item.ConfigurationToolItem;
import com.xinyihl.functionalstoragelegacy.common.item.LinkingToolItem;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.misc.Configurations;
import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import com.xinyihl.functionalstoragelegacy.util.ConnectedDrawers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * TileEntity for the storage controller.
 * Aggregates connected drawers into a unified item/fluid handler.
 * Uses BFS (ConnectedDrawers) for discovery and ControllerInventoryHandler/ControllerFluidHandler for access.
 */
public class DrawerControllerTile extends ControllableDrawerTile {

    private static final HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    private final ConnectedDrawers connectedDrawers;
    private final ControllerItemHandler inventoryHandler;
    private final ControllerFluidHandler fluidHandler;
    protected boolean needRebuild = false;

    public DrawerControllerTile() {
        this.drawerOptions = new DrawerOptions();
        this.storageUpgrades = new ItemStackHandler(getStorageUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return canInsertStorageUpgrade(slot, stack);
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!canRemoveStorageUpgrade(slot)) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                needsUpgradeCache = true;
                needRebuild = true;
                markDirty();
            }
        };
        this.utilityUpgrades = new ItemStackHandler(getUtilityUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return canInsertUtilityUpgrade(slot, stack);
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                needsUpgradeCache = true;
                markDirty();
            }
        };
        this.connectedDrawers = new ConnectedDrawers(null, this);
        this.inventoryHandler = new ControllerItemHandler();
        this.fluidHandler = new ControllerFluidHandler();
    }

    private void refreshHandlers() {
        inventoryHandler.setHandlers(connectedDrawers.getItemHandlers());
        fluidHandler.setHandlers(connectedDrawers.getFluidHandlers());
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote) {
            if (world.getTotalWorldTime() % 10 == 0) {
                int expectedSize = connectedDrawers.getConnectedDrawers().size();
                int actualSize = connectedDrawers.getItemHandlers().size() + connectedDrawers.getFluidHandlers().size();
                if (expectedSize != actualSize || needRebuild) {
                    rebuild();
                    needRebuild = false;
                }
            }
        }
    }

    private void rebuild() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(getControllerRange());
        connectedDrawers.getConnectedDrawers().removeIf(
                pos -> {
                    BlockPos pos1 = BlockPos.fromLong(pos);
                    TileEntity tile = world.getTileEntity(pos1);
                    return !(area.contains(new Vec3d(pos1.getX() + 0.5, pos1.getY() + 0.5, pos1.getZ() + 0.5)) && tile instanceof ControllableDrawerTile);
                }
        );
        connectedDrawers.rebuild();
        refreshHandlers();
        markDirty();
        sendUpdatePacket();
    }

    @Override
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        connectedDrawers.setLevel(worldIn);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuild();
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ, int slot) {
        ItemStack heldStack = player.getHeldItem(hand);

        if (heldStack.getItem() instanceof ConfigurationToolItem || heldStack.getItem() == RegistrationHandler.LINKING_TOOL) {
            return false;
        }

        if (!world.isRemote) {
            if (player.isSneaking()) {
                // Open GUI on sneak-click
                player.openGui(FunctionalStorageLegacy.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
            }

            // Insert into locked drawers first
            for (IItemHandler handler : connectedDrawers.getItemHandlers()) {
                if (handler instanceof ILockable && ((ILockable) handler).isLocked()) {
                    for (int s = 0; s < handler.getSlots(); s++) {
                        if (!heldStack.isEmpty() && handler.insertItem(s, heldStack, true).getCount() != heldStack.getCount()) {
                            player.setHeldItem(hand, handler.insertItem(s, heldStack, false));
                            return true;
                        }
                        // Double-click fast insert
                        if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(player.getUniqueID(), System.currentTimeMillis()) < 300) {
                            for (ItemStack itemStack : player.inventory.mainInventory) {
                                if (!itemStack.isEmpty() && handler.insertItem(s, itemStack, true).getCount() != itemStack.getCount()) {
                                    itemStack.setCount(handler.insertItem(s, itemStack.copy(), false).getCount());
                                }
                            }
                        }
                    }
                }

                if (handler instanceof ILockable && !((ILockable) handler).isLocked()) {
                    for (int s = 0; s < handler.getSlots(); s++) {
                        if (!heldStack.isEmpty() && !handler.getStackInSlot(s).isEmpty()
                                && handler.insertItem(s, heldStack, true).getCount() != heldStack.getCount()) {
                            player.setHeldItem(hand, handler.insertItem(s, heldStack, false));
                            return true;
                        }
                        if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(player.getUniqueID(), System.currentTimeMillis()) < 300) {
                            for (ItemStack itemStack : player.inventory.mainInventory) {
                                if (!itemStack.isEmpty() && !handler.getStackInSlot(s).isEmpty()
                                        && handler.insertItem(s, itemStack, true).getCount() != itemStack.getCount()) {
                                    itemStack.setCount(handler.insertItem(s, itemStack.copy(), false).getCount());
                                }
                            }
                        }
                    }
                }
            }

            INTERACTION_LOGGER.put(player.getUniqueID(), System.currentTimeMillis());
        }

        return true;
    }

    @Override
    public IItemHandler getItemHandler() {
        return inventoryHandler;
    }

    public ControllerItemHandler getControllerItemHandler() {
        return inventoryHandler;
    }

    public ControllerFluidHandler getControllerFluidHandler() {
        return fluidHandler;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventoryHandler);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void toggleLocking() {
        super.toggleLocking();
        if (world != null && !world.isRemote) {
            for (Long drawerPos : new ArrayList<>(connectedDrawers.getConnectedDrawers())) {
                TileEntity te = world.getTileEntity(BlockPos.fromLong(drawerPos));
                if (te instanceof DrawerControllerTile) continue;
                if (te instanceof ControllableDrawerTile) {
                    ((ControllableDrawerTile) te).setLocked(this.isLocked());
                }
            }
        }
    }

    @Override
    public void toggleOption(ConfigurationToolItem.ConfigurationAction action) {
        super.toggleOption(action);
        if (world != null && !world.isRemote) {
            for (Long drawerPos : new ArrayList<>(connectedDrawers.getConnectedDrawers())) {
                TileEntity te = world.getTileEntity(BlockPos.fromLong(drawerPos));
                if (te instanceof DrawerControllerTile) continue;
                if (te instanceof ControllableDrawerTile) {
                    ControllableDrawerTile cdt = (ControllableDrawerTile) te;
                    if (action.getMax() == 1) {
                        cdt.getDrawerOptions().setActive(action, this.getDrawerOptions().isActive(action));
                    } else {
                        cdt.getDrawerOptions().setAdvancedValue(action, this.getDrawerOptions().getAdvancedValue(action));
                    }
                    cdt.markDirty();
                    cdt.sendUpdatePacket();
                }
            }
        }
    }

    /**
     * Get the effective controller search range.
     * Base range from config multiplied by range fraction from storage upgrades.
     */
    public double getControllerRange() {
        return Configurations.GENERAL.drawerControllerLinkingRange + getRangeBonus();
    }

    public boolean addConnectedDrawers(LinkingToolItem.ActionMode action, BlockPos... positions) {
        double range = getControllerRange();
        boolean didWork = false;
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(range);

        for (BlockPos position : positions) {
            // Skip controller blocks (don't link controllers to themselves)
            if (world.getBlockState(position).getBlock() == RegistrationHandler.DRAWER_CONTROLLER_BLOCK) {
                continue;
            }

            TileEntity te = world.getTileEntity(position);
            if (te instanceof ControllerExtensionTile) {
                connectedDrawers.removeLinkedExtension(position);

                if (action == LinkingToolItem.ActionMode.ADD) {
                    if (area.contains(new net.minecraft.util.math.Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5))) {
                        ((ControllerExtensionTile) te).setControllerPos(this.pos);
                        connectedDrawers.addLinkedExtension(position);
                        didWork = true;
                    }
                } else {
                    ((ControllerExtensionTile) te).clearControllerPos();
                    didWork = true;
                }
                continue;
            }

            if (te instanceof ControllableDrawerTile) {
                if (action == LinkingToolItem.ActionMode.ADD) {
                    if (area.contains(new net.minecraft.util.math.Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5))) {
                        ((ControllableDrawerTile) te).setControllerPos(this.pos);
                        long posLong = position.toLong();
                        if (!connectedDrawers.getConnectedDrawers().contains(posLong)) {
                            connectedDrawers.getConnectedDrawers().add(posLong);
                            didWork = true;
                        }
                    }
                } else if (action == LinkingToolItem.ActionMode.REMOVE) {
                    connectedDrawers.getConnectedDrawers().removeIf(l -> l == position.toLong());
                    ((ControllableDrawerTile) te).clearControllerPos();
                    didWork = true;
                }
            }
        }

        connectedDrawers.rebuild();
        refreshHandlers();
        markDirty();
        sendUpdatePacket();
        return didWork;
    }

    @Override
    protected void writeCustomData(NBTTagCompound nbt) {
        nbt.setTag("ConnectedDrawers", connectedDrawers.serializeNBT());
    }

    @Override
    protected void readCustomData(NBTTagCompound nbt) {
        if (nbt.hasKey("ConnectedDrawers")) {
            connectedDrawers.deserializeNBT(nbt.getCompoundTag("ConnectedDrawers"));
        } else {
            connectedDrawers.deserializeNBT(nbt);
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        compound.setTag("ConnectedDrawers", connectedDrawers.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("ConnectedDrawers")) {
            connectedDrawers.deserializeNBT(compound.getCompoundTag("ConnectedDrawers"));
        } else {
            connectedDrawers.deserializeNBT(compound);
        }
    }

    @Override
    public int getUtilityUpgradesAmount() {
        return 0;
    }

    public ConnectedDrawers getConnectedDrawers() {
        return connectedDrawers;
    }

    /**
     * Remove a drawer from the connected list (called when drawer is broken).
     */
    public void removeConnectedDrawer(BlockPos drawerPos) {
        connectedDrawers.getConnectedDrawers().removeIf(l -> l == drawerPos.toLong());
        connectedDrawers.removeLinkedExtension(drawerPos);
        connectedDrawers.rebuild();
        refreshHandlers();
        markDirty();
        sendUpdatePacket();
    }

    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return TileEntity.INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return Float.POSITIVE_INFINITY;
    }
}
