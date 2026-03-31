package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler.CustomFluidTank;
import com.xinyihl.functionalstoragelegacy.common.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import com.xinyihl.functionalstoragelegacy.util.ConnectedDrawers;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * AE2 ME inventory handler aggregating all fluid drawers connected to a controller.
 */
public class ControllerMEFluidHandler implements IDrawerMEInventoryHandler<IAEFluidStack> {

    private final DrawerControllerTile controller;
    private final IFluidStorageChannel channel;

    public ControllerMEFluidHandler(DrawerControllerTile controller, IFluidStorageChannel channel) {
        this.controller = controller;
        this.channel = channel;
    }

    @Override
    public IAEFluidStack injectItems(IAEFluidStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        FluidStack inputFluid = input.getFluidStack();
        long remaining = input.getStackSize();

        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return input;

        // Priority 1: Matching tanks
        for (Long posLong : connected.getConnectedDrawers()) {
            if (remaining <= 0) return null;
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));
            if (!(te instanceof FluidDrawerTile)) continue;

            BigFluidHandler handler = ((FluidDrawerTile) te).getFluidHandler();
            remaining = injectIntoFluidHandler(handler, inputFluid, remaining, type, true);
        }
        if (remaining <= 0) return null;

        // Priority 2: Empty tanks
        for (Long posLong : connected.getConnectedDrawers()) {
            if (remaining <= 0) return null;
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));
            if (!(te instanceof FluidDrawerTile)) continue;

            BigFluidHandler handler = ((FluidDrawerTile) te).getFluidHandler();
            remaining = injectIntoFluidHandler(handler, inputFluid, remaining, type, false);
        }

        if (remaining >= input.getStackSize()) return input;
        if (remaining <= 0) return null;

        IAEFluidStack result = input.copy();
        result.setStackSize(remaining);
        return result;
    }

    private long injectIntoFluidHandler(BigFluidHandler handler, FluidStack inputFluid,
                                        long remaining, Actionable type, boolean requireMatching) {
        if (handler.isCreative()) {
            for (CustomFluidTank tank : handler.getTanks()) {
                FluidStack current = tank.getFluid();
                if (requireMatching) {
                    if (current != null && current.amount > 0 && current.isFluidEqual(inputFluid)) {
                        if (type == Actionable.MODULATE) {
                            current.amount = handler.getCapacityPerTank();
                            tank.setFluid(current);
                            handler.onChange();
                        }
                        return 0;
                    }
                } else {
                    if (current == null || current.amount <= 0) {
                        if (handler.isLocked() && tank.getLockedFluid() != null
                                && !tank.getLockedFluid().isFluidEqual(inputFluid)) continue;
                        if (type == Actionable.MODULATE) {
                            FluidStack full = inputFluid.copy();
                            full.amount = handler.getCapacityPerTank();
                            tank.setFluid(full);
                            tank.setLockedFluid(inputFluid.copy());
                            handler.onChange();
                        }
                        return 0;
                    }
                }
            }
            return remaining;
        }

        for (CustomFluidTank tank : handler.getTanks()) {
            FluidStack current = tank.getFluid();

            if (requireMatching) {
                if (current == null || current.amount <= 0 || !current.isFluidEqual(inputFluid)) continue;
            } else {
                if (current != null && current.amount > 0) continue;
                if (handler.isLocked() && tank.getLockedFluid() != null
                        && !tank.getLockedFluid().isFluidEqual(inputFluid)) continue;
            }

            int capacity = tank.getCapacity();
            int currentAmount = (current != null) ? current.amount : 0;
            long space = capacity - currentAmount;
            long inserting = Math.min(remaining, space);

            if (inserting > 0) {
                if (type == Actionable.MODULATE) {
                    if (current == null || current.amount <= 0) {
                        FluidStack filled = inputFluid.copy();
                        filled.amount = (int) inserting;
                        tank.setFluid(filled);
                    } else {
                        current.amount += (int) inserting;
                        tank.setFluid(current);
                    }
                    handler.onChange();
                }
                remaining -= inserting;
            }

            if (handler.isVoid() && requireMatching && remaining > 0) return 0;
            if (remaining <= 0) return 0;
        }
        return remaining;
    }

    @Override
    public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, IActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;

        FluidStack requestFluid = request.getFluidStack();
        long toExtract = request.getStackSize();
        long extracted = 0;

        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return null;

        for (Long posLong : connected.getConnectedDrawers()) {
            if (extracted >= toExtract) break;
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));
            if (!(te instanceof FluidDrawerTile)) continue;

            BigFluidHandler handler = ((FluidDrawerTile) te).getFluidHandler();
            for (CustomFluidTank tank : handler.getTanks()) {
                FluidStack current = tank.getFluid();
                if (current == null || current.amount <= 0 || !current.isFluidEqual(requestFluid)) continue;

                long available = handler.isCreative() ? (toExtract - extracted) : current.amount;
                long extracting = Math.min(toExtract - extracted, available);
                if (extracting > 0) {
                    if (mode == Actionable.MODULATE && !handler.isCreative()) {
                        current.amount -= (int) extracting;
                        if (current.amount <= 0) tank.setFluid(null);
                        handler.onChange();
                    }
                    extracted += extracting;
                }
            }
        }

        if (extracted <= 0) return null;

        IAEFluidStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out) {
        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return out;

        for (Long posLong : connected.getConnectedDrawers()) {
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));
            if (!(te instanceof FluidDrawerTile)) continue;

            BigFluidHandler handler = ((FluidDrawerTile) te).getFluidHandler();
            for (CustomFluidTank tank : handler.getTanks()) {
                FluidStack current = tank.getFluid();
                if (current != null && current.amount > 0) {
                    IAEFluidStack aeStack = channel.createStack(current);
                    if (aeStack != null) {
                        aeStack.setStackSize(handler.isCreative() ? Long.MAX_VALUE : current.amount);
                        out.addStorage(aeStack);
                    }
                }
            }
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEFluidStack> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEFluidStack input) {
        return false;
    }

    @Override
    public boolean canAccept(IAEFluidStack input) {
        if (input == null) return false;
        FluidStack inputFluid = input.getFluidStack();

        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return false;

        for (Long posLong : connected.getConnectedDrawers()) {
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));
            if (!(te instanceof FluidDrawerTile)) continue;

            BigFluidHandler handler = ((FluidDrawerTile) te).getFluidHandler();
            for (CustomFluidTank tank : handler.getTanks()) {
                FluidStack current = tank.getFluid();
                if (current == null || current.amount <= 0) {
                    if (handler.isLocked() && tank.getLockedFluid() != null) {
                        if (tank.getLockedFluid().isFluidEqual(inputFluid)) return true;
                        continue;
                    }
                    return true;
                }
                if (current.isFluidEqual(inputFluid)) {
                    if (current.amount < tank.getCapacity() || handler.isVoid()) return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int pass) {
        return true;
    }
}
