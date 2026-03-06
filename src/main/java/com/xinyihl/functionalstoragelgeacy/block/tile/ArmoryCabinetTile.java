package com.xinyihl.functionalstoragelgeacy.block.tile;

import com.xinyihl.functionalstoragelgeacy.inventory.ArmoryCabinetInventoryHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TileEntity for the armory cabinet.
 * Stores unstackable items (armor, weapons, tools, discs, etc.).
 * Does not extend ControllableDrawerTile since it has no upgrades/controller support.
 */
public class ArmoryCabinetTile extends TileEntity {

    private final ArmoryCabinetInventoryHandler handler;

    public ArmoryCabinetTile() {
        this.handler = new ArmoryCabinetInventoryHandler() {
            @Override
            public void onChange() {
                ArmoryCabinetTile.this.markDirty();
            }
        };
    }

    public IItemHandler getStorage() {
        return handler;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        compound.setTag("Inventory", handler.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            handler.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
    }

    public boolean isEverythingEmpty() {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save tile data to NBT for item storage.
     */
    public NBTTagCompound saveTileToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Inventory", handler.serializeNBT());
        return nbt;
    }

    /**
     * Load tile data from item NBT.
     */
    public void loadTileFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("Inventory")) {
            handler.deserializeNBT(nbt.getCompoundTag("Inventory"));
        }
        markDirty();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
        }
        return super.getCapability(capability, facing);
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(@Nonnull NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
