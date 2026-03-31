package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.AEApi;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.capabilities.Capabilities;
import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigFluidHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.WoodDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.CompactingDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import net.minecraftforge.common.capabilities.Capability;

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
                    new DrawerMEMonitor<>(new ControllerMEItemHandler(controller, itemChannel), itemChannel),
                    new DrawerMEMonitor<>(new ControllerMEFluidHandler(controller, fluidChannel), fluidChannel)
            );
        }

        if (tile instanceof WoodDrawerTile) {
            WoodDrawerTile drawer = (WoodDrawerTile) tile;
            BigInventoryHandler handler = drawer.getHandler();
            return new DrawerStorageAccessor(
                    new DrawerMEMonitor<>(new DrawerMEItemHandler(handler, itemChannel), itemChannel),
                    null
            );
        }

        if (tile instanceof CompactingDrawerTile) {
            CompactingDrawerTile drawer = (CompactingDrawerTile) tile;
            CompactingInventoryHandler handler = drawer.getCompactingHandler();
            return new DrawerStorageAccessor(
                    new DrawerMEMonitor<>(new CompactingMEItemHandler(handler, itemChannel), itemChannel),
                    null
            );
        }

        if (tile instanceof FluidDrawerTile) {
            FluidDrawerTile drawer = (FluidDrawerTile) tile;
            BigFluidHandler handler = drawer.getFluidHandler();
            return new DrawerStorageAccessor(
                    null,
                    new DrawerMEMonitor<>(new DrawerMEFluidHandler(handler, fluidChannel), fluidChannel)
            );
        }

        return null;
    }
}
