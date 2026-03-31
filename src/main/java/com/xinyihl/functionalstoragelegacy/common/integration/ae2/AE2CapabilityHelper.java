package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.AEApi;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.capabilities.Capabilities;
import com.xinyihl.functionalstoragelegacy.api.IBigFluidHandler;
import com.xinyihl.functionalstoragelegacy.api.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.CompactingDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Helper class that directly references AE2 classes.
 * Only loaded when AE2 is present.
 */
public class AE2CapabilityHelper {

    public static boolean isStorageAccessor(Capability<?> capability) {
        return capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR;
    }

    @SuppressWarnings("unchecked")
    public static <T> T castAccessor(IStorageMonitorableAccessor accessor) {
        return (T) Capabilities.STORAGE_MONITORABLE_ACCESSOR.cast(accessor);
    }

    public static Object createAccessor(ControllableDrawerTile tile) {
        IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IFluidStorageChannel fluidChannel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);

        if (tile instanceof DrawerControllerTile) {
            DrawerControllerTile controller = (DrawerControllerTile) tile;
            return new DrawerStorageAccessor(
                    new DrawerMEMonitor<>(new ControllerMEItemHandler(controller.getControllerItemHandler(), itemChannel), itemChannel),
                    new DrawerMEMonitor<>(new ControllerMEFluidHandler(controller.getControllerFluidHandler(), fluidChannel), fluidChannel)
            );
        }

        // Fluid drawers
        if (tile instanceof FluidDrawerTile) {
            IFluidHandler fluidHandler = ((FluidDrawerTile) tile).getFluidHandler();
            if (fluidHandler instanceof IBigFluidHandler) {
                return new DrawerStorageAccessor(
                        null,
                        new DrawerMEMonitor<>(new DrawerMEFluidHandler((IBigFluidHandler) fluidHandler, fluidChannel), fluidChannel)
                );
            }
            return null;
        }

        // Item drawers: get IItemHandler and check for IBigItemHandler
        IItemHandler itemHandler = tile.getItemHandler();
        if (itemHandler instanceof IBigItemHandler) {
            IBigItemHandler bigHandler = (IBigItemHandler) itemHandler;
            if (tile instanceof CompactingDrawerTile) {
                return new DrawerStorageAccessor(
                        new DrawerMEMonitor<>(new CompactingMEItemHandler(bigHandler, itemChannel), itemChannel),
                        null
                );
            }
            return new DrawerStorageAccessor(
                    new DrawerMEMonitor<>(new DrawerMEItemHandler(bigHandler, itemChannel), itemChannel),
                    null
            );
        }

        return null;
    }
}
