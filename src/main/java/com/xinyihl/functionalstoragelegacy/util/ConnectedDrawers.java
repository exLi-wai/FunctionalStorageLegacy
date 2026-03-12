package com.xinyihl.functionalstoragelegacy.util;

import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.ControllerExtensionTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import com.xinyihl.functionalstoragelegacy.misc.FunctionalStorageConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.*;

/**
 * Manages the set of drawers connected to a controller.
 * Uses BFS to find all connected drawers within range.
 */
public class ConnectedDrawers {

    private final List<Long> connectedDrawerPositions;
    private final List<IItemHandler> itemHandlers;
    private final List<IFluidHandler> fluidHandlers;
    private BlockPos controllerPos;
    private World world;

    public ConnectedDrawers() {
        this.connectedDrawerPositions = new ArrayList<>();
        this.itemHandlers = new ArrayList<>();
        this.fluidHandlers = new ArrayList<>();
    }

    public ConnectedDrawers(World world, TileEntity controller) {
        this();
        this.world = world;
        if (controller != null) {
            this.controllerPos = controller.getPos();
        }
    }

    public void setController(World world, BlockPos controllerPos) {
        this.world = world;
        this.controllerPos = controllerPos;
    }

    public void setLevel(World world) {
        this.world = world;
    }

    /**
     * Get the list of connected drawer positions as longs.
     */
    public List<Long> getConnectedDrawers() {
        return connectedDrawerPositions;
    }

    /**
     * Get connected positions as BlockPos list.
     */
    public List<BlockPos> getConnectedPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (Long l : connectedDrawerPositions) {
            positions.add(BlockPos.fromLong(l));
        }
        return positions;
    }

    /**
     * Rebuild the connected drawers list using BFS from the controller position.
     */
    public void rebuild() {
        itemHandlers.clear();
        fluidHandlers.clear();

        if (world == null || controllerPos == null) return;

        Iterator<Long> iterator = connectedDrawerPositions.iterator();
        while (iterator.hasNext()) {
            Long posLong = iterator.next();
            BlockPos pos = BlockPos.fromLong(posLong);
            TileEntity te = world.getTileEntity(pos);
            if (!isConnectableDrawer(te)) {
                iterator.remove();
                continue;
            }
            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if (ih != null) itemHandlers.add(ih);
            }
            if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                if (fh != null) fluidHandlers.add(fh);
            }
        }
    }

    /**
     * Scan from controller using BFS. Replaces any stored positions.
     */
    public void scan() {
        connectedDrawerPositions.clear();
        itemHandlers.clear();
        fluidHandlers.clear();

        if (world == null || controllerPos == null) return;

        int range = FunctionalStorageConfig.DRAWER_CONTROLLER_LINKING_RANGE;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(controllerPos);
        visited.add(controllerPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);

                if (visited.contains(neighbor)) continue;
                if (neighbor.distanceSq(controllerPos) > range * range) continue;

                visited.add(neighbor);

                TileEntity te = world.getTileEntity(neighbor);
                if (isConnectableDrawer(te)) {
                    connectedDrawerPositions.add(neighbor.toLong());

                    if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                        IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                        if (ih != null) itemHandlers.add(ih);
                    }
                    if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                        IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                        if (fh != null) fluidHandlers.add(fh);
                    }

                    queue.add(neighbor);
                }
            }
        }
    }

    private boolean isConnectableDrawer(TileEntity te) {
        return te instanceof ControllableDrawerTile
                && !(te instanceof DrawerControllerTile)
                && !(te instanceof ControllerExtensionTile);
    }

    /**
     * Add a single drawer position.
     */
    public void addDrawer(BlockPos pos) {
        long posLong = pos.toLong();
        if (!connectedDrawerPositions.contains(posLong)) {
            connectedDrawerPositions.add(posLong);
        }
        rebuild();
    }

    /**
     * Remove a single drawer position.
     */
    public void removeDrawer(BlockPos pos) {
        connectedDrawerPositions.removeIf(l -> l == pos.toLong());
        rebuild();
    }

    public List<IItemHandler> getItemHandlers() {
        return itemHandlers;
    }

    public List<IFluidHandler> getFluidHandlers() {
        return fluidHandlers;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Long posLong : connectedDrawerPositions) {
            list.appendTag(new NBTTagLong(posLong));
        }
        nbt.setTag("Positions", list);
        if (controllerPos != null) {
            nbt.setLong("ControllerPos", controllerPos.toLong());
        }
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        connectedDrawerPositions.clear();
        if (nbt.hasKey("Positions")) {
            NBTTagList list = nbt.getTagList("Positions", Constants.NBT.TAG_LONG);
            for (int i = 0; i < list.tagCount(); i++) {
                connectedDrawerPositions.add(((NBTTagLong) list.get(i)).getLong());
            }
        }
        if (nbt.hasKey("ControllerPos")) {
            controllerPos = BlockPos.fromLong(nbt.getLong("ControllerPos"));
        }
    }
}
