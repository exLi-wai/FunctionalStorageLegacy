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
 * Supports detecting external inventory changes via {@link #forceUpdate()}.
 */
public class DrawerMEMonitor<T extends IAEStack<T>> implements IMEMonitor<T> {

    private final IMEInventoryHandler<T> handler;
    private final IStorageChannel<T> channel;
    private final Map<IMEMonitorHandlerReceiver<T>, Object> listeners = new HashMap<>();
    private IItemList<T> cachedList;
    private boolean suppressExternalNotify = false;

    public DrawerMEMonitor(IMEInventoryHandler<T> handler, IStorageChannel<T> channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public T injectItems(T input, Actionable type, IActionSource src) {
        suppressExternalNotify = true;
        T result = handler.injectItems(input, type, src);
        suppressExternalNotify = false;
        if (type == Actionable.MODULATE) {
            long injected = input.getStackSize() - (result != null ? result.getStackSize() : 0);
            if (injected > 0) {
                T diff = input.copy();
                diff.setStackSize(injected);
                notifyListeners(diff, src);
            }
            refreshCache();
        }
        return result;
    }

    @Override
    public T extractItems(T request, Actionable mode, IActionSource src) {
        suppressExternalNotify = true;
        T result = handler.extractItems(request, mode, src);
        suppressExternalNotify = false;
        if (mode == Actionable.MODULATE && result != null) {
            T diff = result.copy();
            diff.setStackSize(-result.getStackSize());
            notifyListeners(diff, src);
            refreshCache();
        }
        return result;
    }

    /**
     * Detect external inventory changes (e.g. hopper, pipe, player interaction)
     * by comparing current state against cached snapshot.
     * Called from the tile entity when the underlying inventory changes.
     */
    public void forceUpdate() {
        if (suppressExternalNotify || listeners.isEmpty()) return;

        IItemList<T> currentList = channel.createList();
        handler.getAvailableItems(currentList);

        if (cachedList == null) {
            cachedList = currentList;
            return;
        }

        List<T> changes = new ArrayList<>();

        // Find increases and new items
        for (T current : currentList) {
            T old = cachedList.findPrecise(current);
            long oldSize = old != null ? old.getStackSize() : 0;
            long diff = current.getStackSize() - oldSize;
            if (diff != 0) {
                T change = current.copy();
                change.setStackSize(diff);
                changes.add(change);
            }
        }

        // Find removed items
        for (T old : cachedList) {
            T current = currentList.findPrecise(old);
            if (current == null) {
                T change = old.copy();
                change.setStackSize(-old.getStackSize());
                changes.add(change);
            }
        }

        cachedList = currentList;

        if (!changes.isEmpty()) {
            postChanges(changes, null);
        }
    }

    private void refreshCache() {
        cachedList = channel.createList();
        handler.getAvailableItems(cachedList);
    }

    @Override
    public IItemList<T> getAvailableItems(IItemList<T> out) {
        return handler.getAvailableItems(out);
    }

    @Override
    public IItemList<T> getStorageList() {
        IItemList<T> list = channel.createList();
        getAvailableItems(list);
        refreshCache();
        return list;
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
        postChanges(Collections.singletonList(changed), src);
    }

    private void postChanges(Iterable<T> changes, IActionSource src) {
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
