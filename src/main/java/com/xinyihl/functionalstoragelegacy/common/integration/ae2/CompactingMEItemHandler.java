package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler.Result;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * AE2 ME inventory handler wrapping a CompactingInventoryHandler.
 * Supports long-level amounts by directly manipulating totalInBase.
 * Each compacting tier exposes as a separate entry in AE2.
 */
public class CompactingMEItemHandler implements IDrawerMEInventoryHandler<IAEItemStack> {

    private final CompactingInventoryHandler handler;
    private final IItemStorageChannel channel;

    public CompactingMEItemHandler(CompactingInventoryHandler handler, IItemStorageChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0 || !handler.isSetup()) return input;

        ItemStack inputStack = input.getDefinition();
        long toInsert = input.getStackSize();
        List<Result> results = handler.getResults();

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (result.getStack().isEmpty()) continue;
            if (!ItemUtil.areItemStacksEqual(result.getStack(), inputStack)) continue;

            long baseEquiv = toInsert * result.getNeeded();
            long maxBase = handler.getTotalCapacity();
            long currentBase = handler.getTotalInBase();
            long canInsertBase = maxBase - currentBase;

            if (canInsertBase <= 0) {
                if (handler.isVoid()) return null;
                return input;
            }

            long insertedBase = Math.min(baseEquiv, canInsertBase);
            // Round down to whole items at this tier
            long insertedItems = insertedBase / result.getNeeded();
            if (insertedItems <= 0) {
                if (handler.isVoid()) return null;
                return input;
            }

            if (type == Actionable.MODULATE && !handler.isCreative()) {
                handler.setTotalInBase(currentBase + insertedItems * result.getNeeded());
                handler.onChange();
            }

            long leftover = toInsert - insertedItems;
            if (leftover <= 0 || handler.isVoid()) return null;

            IAEItemStack remainder = input.copy();
            remainder.setStackSize(leftover);
            return remainder;
        }

        // Not a matching item
        return input;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
        if (request == null || request.getStackSize() <= 0 || !handler.isSetup()) return null;

        ItemStack requestStack = request.getDefinition();
        long toExtract = request.getStackSize();
        List<Result> results = handler.getResults();

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (result.getStack().isEmpty()) continue;
            if (!ItemUtil.areItemStacksEqual(result.getStack(), requestStack)) continue;

            long totalBase = handler.getTotalInBase();
            long available = handler.isCreative() ? toExtract : totalBase / result.getNeeded();
            long extracting = Math.min(toExtract, available);

            if (extracting <= 0) return null;

            if (mode == Actionable.MODULATE && !handler.isCreative()) {
                handler.setTotalInBase(totalBase - extracting * result.getNeeded());
                handler.onChange();
            }

            IAEItemStack extracted = request.copy();
            extracted.setStackSize(extracting);
            return extracted;
        }

        return null;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        if (!handler.isSetup()) return out;

        long totalBase = handler.getTotalInBase();
        List<Result> results = handler.getResults();

        for (Result result : results) {
            if (result.getStack().isEmpty()) continue;
            long amount = handler.isCreative() ? Long.MAX_VALUE : totalBase / result.getNeeded();
            if (amount <= 0) continue;

            IAEItemStack aeStack = channel.createStack(result.getStack());
            if (aeStack != null) {
                aeStack.setStackSize(amount);
                out.addStorage(aeStack);
            }
        }

        return out;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        if (input == null || !handler.isSetup()) return false;
        ItemStack inputStack = input.getDefinition();
        for (Result result : handler.getResults()) {
            if (!result.getStack().isEmpty() && ItemUtil.areItemStacksEqual(result.getStack(), inputStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null || !handler.isSetup()) return false;
        ItemStack inputStack = input.getDefinition();
        for (Result result : handler.getResults()) {
            if (!result.getStack().isEmpty() && ItemUtil.areItemStacksEqual(result.getStack(), inputStack)) {
                if (handler.isCreative() || handler.isVoid()) return true;
                return handler.getTotalInBase() < handler.getTotalCapacity();
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
