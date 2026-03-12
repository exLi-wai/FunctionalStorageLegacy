package com.xinyihl.functionalstoragelegacy.api;

/**
 * Interface for lockable storage handlers.
 * When locked, the drawer retains its filter even when empty.
 */
public interface ILockable {
    boolean isLocked();
}
