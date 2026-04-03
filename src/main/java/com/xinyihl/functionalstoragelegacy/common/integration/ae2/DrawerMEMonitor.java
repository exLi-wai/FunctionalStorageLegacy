package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.ITickingMonitor;

import java.util.*;

/**
 * Generic IMEMonitor implementation wrapping an IMEInventoryHandler.
 * Implements ITickingMonitor so AE2 storage bus can poll for external changes
 * (e.g. hopper, pipe, player interaction) via periodic onTick() calls.
 * Pattern follows AE2's ItemHandlerAdapter + InventoryCache approach.
 */
public class DrawerMEMonitor<T extends IAEStack<T>> implements IMEMonitor<T>, ITickingMonitor {

    private final IMEInventoryHandler<T> handler;
    private final IStorageChannel<T> channel;
    private final Map<IMEMonitorHandlerReceiver<T>, Object> listeners = new HashMap<>();
    private IActionSource mySource;
    private IItemList<T> cachedList;

    public DrawerMEMonitor(IMEInventoryHandler<T> handler, IStorageChannel<T> channel) {
        this.handler = handler;
        this.channel = channel;
        this.cachedList = channel.createList();
        handler.getAvailableItems(this.cachedList);
    }

    // ---- ITickingMonitor ----

    @Override
    public TickRateModulation onTick() {
        IItemList<T> currentList = channel.createList();
        handler.getAvailableItems(currentList);

        // Compute diff: negate cached, add current, collect non-zero
        for (T cached : cachedList) {
            cached.setStackSize(-cached.getStackSize());
        }
        for (T current : currentList) {
            cachedList.add(current);
        }

        List<T> changes = new ArrayList<>();
        for (T entry : cachedList) {
            if (entry.getStackSize() != 0) {
                changes.add(entry);
            }
        }

        cachedList = currentList;

        if (!changes.isEmpty()) {
            postDifference(changes);
            return TickRateModulation.URGENT;
        }
        return TickRateModulation.SLOWER;
    }

    @Override
    public void setActionSource(IActionSource source) {
        this.mySource = source;
    }

    // ---- IMEInventory ----

    @Override
    public T injectItems(T input, Actionable type, IActionSource src) {
        T result = handler.injectItems(input, type, src);
        if (type == Actionable.MODULATE) {
            long injected = input.getStackSize() - (result != null ? result.getStackSize() : 0);
            if (injected > 0) {
                T diff = input.copy();
                diff.setStackSize(injected);
                cachedList.add(diff);
                postDifference(Collections.singletonList(diff));
            }
        }
        return result;
    }

    @Override
    public T extractItems(T request, Actionable mode, IActionSource src) {
        T result = handler.extractItems(request, mode, src);
        if (mode == Actionable.MODULATE && result != null) {
            T diff = result.copy();
            diff.setStackSize(-result.getStackSize());
            cachedList.add(diff);
            postDifference(Collections.singletonList(diff));
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

    // ---- IMEInventoryHandler delegated ----

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

    // ---- IBaseMonitor ----

    @Override
    public void addListener(IMEMonitorHandlerReceiver<T> l, Object verificationToken) {
        listeners.put(l, verificationToken);
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<T> l) {
        listeners.remove(l);
    }

    // ---- Internal ----

    private void postDifference(Iterable<T> changes) {
        Iterator<Map.Entry<IMEMonitorHandlerReceiver<T>, Object>> it = listeners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver<T>, Object> entry = it.next();
            IMEMonitorHandlerReceiver<T> receiver = entry.getKey();
            if (receiver.isValid(entry.getValue())) {
                receiver.postChange(this, changes, mySource);
            } else {
                it.remove();
            }
        }
    }
}
