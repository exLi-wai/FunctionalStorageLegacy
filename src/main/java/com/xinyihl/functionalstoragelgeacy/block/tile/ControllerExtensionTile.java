package com.xinyihl.functionalstoragelgeacy.block.tile;

import com.xinyihl.functionalstoragelgeacy.fluid.ControllerFluidHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.ControllerInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.util.ConnectedDrawers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TileEntity for controller extension blocks.
 * Delegates to its linked controller's connected drawers.
 * Provides the same capability interface as the controller.
 */
public class ControllerExtensionTile extends ControllableDrawerTile {

    private BlockPos linkedControllerPos;
    private final ControllerInventoryHandler inventoryHandler;
    private final ControllerFluidHandler fluidHandler;

    public ControllerExtensionTile() {
        super();
        this.inventoryHandler = new ControllerInventoryHandler();
        this.fluidHandler = new ControllerFluidHandler();
    }

    private void refreshHandlers() {
        ConnectedDrawers drawers = getLinkedDrawers();
        inventoryHandler.setHandlers(drawers.getItemHandlers());
        fluidHandler.setHandlers(drawers.getFluidHandlers());
    }

    private ConnectedDrawers getLinkedDrawers() {
        if (linkedControllerPos != null && world != null) {
            TileEntity te = world.getTileEntity(linkedControllerPos);
            if (te instanceof StorageControllerTile) {
                return ((StorageControllerTile) te).getConnectedDrawers();
            }
        }
        return new ConnectedDrawers();
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote && world.getTotalWorldTime() % 20 == 0) {
            // Auto-find nearby controller if not linked
            if (linkedControllerPos == null) {
                for (EnumFacing facing : EnumFacing.VALUES) {
                    BlockPos neighborPos = pos.offset(facing);
                    TileEntity te = world.getTileEntity(neighborPos);
                    if (te instanceof StorageControllerTile) {
                        linkedControllerPos = neighborPos;
                        markDirty();
                        break;
                    }
                    if (te instanceof ControllerExtensionTile) {
                        ControllerExtensionTile ext = (ControllerExtensionTile) te;
                        if (ext.linkedControllerPos != null) {
                            linkedControllerPos = ext.linkedControllerPos;
                            markDirty();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public IItemHandler getItemHandler() {
        return inventoryHandler;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (linkedControllerPos != null) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (linkedControllerPos != null) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventoryHandler);
            }
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
            }
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected void writeCustomData(NBTTagCompound nbt) {
        if (linkedControllerPos != null) {
            nbt.setLong("LinkedController", linkedControllerPos.toLong());
        }
    }

    @Override
    protected void readCustomData(NBTTagCompound nbt) {
        if (nbt.hasKey("LinkedController")) {
            linkedControllerPos = BlockPos.fromLong(nbt.getLong("LinkedController"));
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        if (linkedControllerPos != null) {
            compound.setLong("LinkedController", linkedControllerPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("LinkedController")) {
            linkedControllerPos = BlockPos.fromLong(compound.getLong("LinkedController"));
        }
    }

    @Override
    public int getStorageUpgradesAmount() {
        return 0;
    }

    @Override
    public int getUtilityUpgradesAmount() {
        return 0;
    }

    public void setLinkedControllerPos(BlockPos pos) {
        this.linkedControllerPos = pos;
        markDirty();
    }

    public BlockPos getLinkedControllerPos() {
        return linkedControllerPos;
    }
}
