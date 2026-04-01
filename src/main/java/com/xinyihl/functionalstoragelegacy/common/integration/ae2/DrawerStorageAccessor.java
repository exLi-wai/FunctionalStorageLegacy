package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

import javax.annotation.Nullable;

/**
 * IStorageMonitorableAccessor implementation for drawers.
 * Provides item and/or fluid ME monitors to AE2 storage bus.
 */
public class DrawerStorageAccessor implements IStorageMonitorableAccessor {

    private final DrawerMEMonitor<IAEItemStack> itemMonitor;
    private final DrawerMEMonitor<IAEFluidStack> fluidMonitor;

    public DrawerStorageAccessor(@Nullable DrawerMEMonitor<IAEItemStack> itemMonitor,
                                 @Nullable DrawerMEMonitor<IAEFluidStack> fluidMonitor) {
        this.itemMonitor = itemMonitor;
        this.fluidMonitor = fluidMonitor;
    }

    @Override
    public IStorageMonitorable getInventory(IActionSource src) {
        return new IStorageMonitorable() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> storageChannel) {
                if (itemMonitor != null && storageChannel == itemMonitor.getChannel()) {
                    return (IMEMonitor<T>) itemMonitor;
                }
                if (fluidMonitor != null && storageChannel == fluidMonitor.getChannel()) {
                    return (IMEMonitor<T>) fluidMonitor;
                }
                return null;
            }
        };
    }

    /**
     * Notify the AE2 monitors that the underlying inventory has changed externally.
     * Called when items/fluids are inserted or extracted outside of the ME system.
     */
    public void notifyChange() {
        if (itemMonitor != null) itemMonitor.forceUpdate();
        if (fluidMonitor != null) fluidMonitor.forceUpdate();
    }
}
