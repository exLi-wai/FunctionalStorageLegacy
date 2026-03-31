package com.xinyihl.functionalstoragelegacy.api;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Extended fluid handler interface supporting long-level throughput.
 * Extends IFluidHandler to maintain compatibility with Forge's capability system.
 */
public interface IBigFluidHandler extends IFluidHandler {

    /**
     * Get the number of tanks.
     */
    int getTanksCount();

    /**
     * Get the capacity of a tank as a long value.
     */
    long getLongCapacity(int tank);

    /**
     * Fill a specific tank with long-level amounts.
     *
     * @param tank     The tank index to fill
     * @param resource The fluid type to fill
     * @param amount   The amount to fill (long)
     * @param simulate If true, the fill is only simulated
     * @return The amount that was actually filled
     */
    long fillLong(int tank, FluidStack resource, long amount, boolean simulate);

    /**
     * Drain from a specific tank with long-level amounts.
     *
     * @param tank     The tank index to drain from
     * @param amount   The maximum amount to drain (long)
     * @param simulate If true, the drain is only simulated
     * @return The amount that was actually drained
     */
    long drainLong(int tank, long amount, boolean simulate);

    /**
     * Get the fluid stored in a tank (type information, amount may be int-limited).
     */
    @Nullable
    FluidStack getStoredFluid(int tank);

    /**
     * Get the stored fluid amount in a tank as a long value.
     */
    long getStoredFluidAmount(int tank);

    /**
     * Whether this handler has the void upgrade.
     */
    boolean isVoid();

    /**
     * Whether this handler is locked.
     */
    boolean isLocked();

    /**
     * Whether this handler is in creative mode.
     */
    boolean isCreative();
}
