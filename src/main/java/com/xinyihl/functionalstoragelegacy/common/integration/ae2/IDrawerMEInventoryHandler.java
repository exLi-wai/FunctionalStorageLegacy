package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;

/**
 * Marker interface for drawer ME inventory handlers.
 */
public interface IDrawerMEInventoryHandler<T extends IAEStack<T>> extends IMEInventoryHandler<T> {
}
