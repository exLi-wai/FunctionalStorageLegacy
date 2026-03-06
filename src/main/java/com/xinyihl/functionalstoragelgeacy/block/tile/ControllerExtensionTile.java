package com.xinyihl.functionalstoragelgeacy.block.tile;

import com.xinyihl.functionalstoragelgeacy.fluid.ControllerFluidHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.ControllerInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.util.ConnectedDrawers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * TileEntity for controller extension blocks.
 * Delegates to its linked controller's connected drawers.
 * Provides the same capability interface as the controller.
 */
public class ControllerExtensionTile extends ControllableDrawerTile {

    private final ControllerInventoryHandler inventoryHandler;
    private final ControllerFluidHandler fluidHandler;

    public ControllerExtensionTile() {
        super();
        this.inventoryHandler = new ControllerInventoryHandler();
        this.fluidHandler = new ControllerFluidHandler();
    }

    private void clearHandlers() {
        inventoryHandler.setHandlers(Collections.emptyList());
        fluidHandler.setHandlers(Collections.emptyList());
    }

    private void refreshHandlers() {
        ConnectedDrawers drawers = getLinkedDrawers();
        inventoryHandler.setHandlers(drawers.getItemHandlers());
        fluidHandler.setHandlers(drawers.getFluidHandlers());
    }

    @Nullable
    private StorageControllerTile getLinkedController() {
        if (world == null) {
            return null;
        }

        if (controllerPos != null) {
            TileEntity te = world.getTileEntity(controllerPos);
            if (te instanceof StorageControllerTile) {
                return (StorageControllerTile) te;
            }
            return null;
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.offset(facing);
            TileEntity te = world.getTileEntity(neighborPos);
            if (te instanceof StorageControllerTile) {
                setControllerPos(neighborPos);
                return (StorageControllerTile) te;
            }
            if (te instanceof ControllerExtensionTile) {
                BlockPos neighborControllerPos = ((ControllerExtensionTile) te).getControllerPos();
                if (neighborControllerPos != null) {
                    TileEntity controllerTE = world.getTileEntity(neighborControllerPos);
                    if (controllerTE instanceof StorageControllerTile) {
                        setControllerPos(neighborControllerPos);
                        return (StorageControllerTile) controllerTE;
                    }
                }
            }
        }

        return null;
    }

    private ConnectedDrawers getLinkedDrawers() {
        StorageControllerTile controller = getLinkedController();
        if (controller != null) {
            ConnectedDrawers drawers = controller.getConnectedDrawers();
            drawers.setLevel(world);
            drawers.rebuild();
            return drawers;
        }
        return new ConnectedDrawers();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            if (getLinkedController() != null) {
                refreshHandlers();
            } else {
                clearHandlers();
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote && world.getTotalWorldTime() % 20 == 0) {
            if (getLinkedController() != null) {
                refreshHandlers();
            } else {
                clearHandlers();
            }
        }
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ, int slot) {
        StorageControllerTile controller = getLinkedController();
        if (controller != null) {
            return controller.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
        }
        return super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
    }

    @Override
    public IItemHandler getItemHandler() {
        return inventoryHandler;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (controllerPos != null) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (controllerPos != null) {
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
        if (controllerPos != null) {
            nbt.setLong("LinkedController", controllerPos.toLong());
        }
    }

    @Override
    protected void readCustomData(NBTTagCompound nbt) {
        if (nbt.hasKey("LinkedController")) {
            controllerPos = BlockPos.fromLong(nbt.getLong("LinkedController"));
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        if (controllerPos != null) {
            compound.setLong("LinkedController", controllerPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("LinkedController") && controllerPos == null) {
            controllerPos = BlockPos.fromLong(compound.getLong("LinkedController"));
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

    @Override
    public void setControllerPos(BlockPos controllerPos) {
        boolean changed = this.controllerPos == null ? controllerPos != null : !this.controllerPos.equals(controllerPos);
        super.setControllerPos(controllerPos);
        if (changed) {
            refreshHandlers();
            sendUpdatePacket();
        }
    }

    @Override
    public void clearControllerPos() {
        if (this.controllerPos == null) {
            return;
        }
        super.clearControllerPos();
        clearHandlers();
        sendUpdatePacket();
    }
}
