package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

import java.util.*;

/**
 * Generic IMEMonitor implementation wrapping an IMEInventoryHandler.
 * Manages change listeners for AE2 network notifications.
 */
public class DrawerMEMonitor<T extends IAEStack<T>> implements IMEMonitor<T> {

    private final IMEInventoryHandler<T> handler;
    private final IStorageChannel<T> channel;
    private final Map<IMEMonitorHandlerReceiver<T>, Object> listeners = new HashMap<>();

    public DrawerMEMonitor(IMEInventoryHandler<T> handler, IStorageChannel<T> channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public T injectItems(T input, Actionable type, IActionSource src) {
        T result = handler.injectItems(input, type, src);
        if (type == Actionable.MODULATE) {
            notifyListeners(input, src);
        }
        return result;
    }

    @Override
    public T extractItems(T request, Actionable mode, IActionSource src) {
        T result = handler.extractItems(request, mode, src);
        if (mode == Actionable.MODULATE && result != null) {
            notifyListeners(result, src);
        }
        return result;
    }

    @Override
    public IItemList<T> getAvailableItems(IItemList<T> out) {
        return handler.getAvailableItems(out);
    }

    @Override
    public IItemList<T> getStorageList() {
        IItemList<T> list = channel.createList();
        return getAvailableItems(list);
    }

    @Override
    public IStorageChannel<T> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return handler.getAccess();
    }

    @Override
    public boolean isPrioritized(T input) {
        return handler.isPrioritized(input);
    }

    @Override
    public boolean canAccept(T input) {
        return handler.canAccept(input);
    }

    @Override
    public int getPriority() {
        return handler.getPriority();
    }

    @Override
    public int getSlot() {
        return handler.getSlot();
    }

    @Override
    public boolean validForPass(int pass) {
        return handler.validForPass(pass);
    }

    @Override
    public void addListener(IMEMonitorHandlerReceiver<T> l, Object verificationToken) {
        listeners.put(l, verificationToken);
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<T> l) {
        listeners.remove(l);
    }

    private void notifyListeners(T changed, IActionSource src) {
        if (changed == null) return;

        List<T> changes = Collections.singletonList(changed);
        Iterator<Map.Entry<IMEMonitorHandlerReceiver<T>, Object>> it = listeners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver<T>, Object> entry = it.next();
            IMEMonitorHandlerReceiver<T> receiver = entry.getKey();
            if (receiver.isValid(entry.getValue())) {
                receiver.postChange(this, changes, src);
            } else {
                it.remove();
            }
        }
    }
}
