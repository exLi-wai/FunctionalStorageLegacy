package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler.BigStack;
import com.xinyihl.functionalstoragelegacy.common.tile.WoodDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.CompactingDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import com.xinyihl.functionalstoragelegacy.util.ConnectedDrawers;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * AE2 ME inventory handler aggregating all item drawers connected to a controller.
 * Iterates connected drawers for long-level item operations.
 */
public class ControllerMEItemHandler implements IDrawerMEInventoryHandler<IAEItemStack> {

    private final DrawerControllerTile controller;
    private final IItemStorageChannel channel;

    public ControllerMEItemHandler(DrawerControllerTile controller, IItemStorageChannel channel) {
        this.controller = controller;
        this.channel = channel;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        if (input == null || input.getStackSize() <= 0) return null;

        ItemStack inputStack = input.getDefinition();
        long remaining = input.getStackSize();

        // Priority 1: Locked drawers with matching items
        remaining = injectIntoDrawers(inputStack, remaining, type, true, true);
        if (remaining <= 0) return null;

        // Priority 2: Non-locked drawers with matching items
        remaining = injectIntoDrawers(inputStack, remaining, type, false, true);
        if (remaining <= 0) return null;

        // Priority 3: Empty slots in non-locked drawers
        remaining = injectIntoDrawers(inputStack, remaining, type, false, false);
        if (remaining <= 0) return null;

        if (remaining >= input.getStackSize()) return input;

        IAEItemStack result = input.copy();
        result.setStackSize(remaining);
        return result;
    }

    private long injectIntoDrawers(ItemStack inputStack, long remaining, Actionable type,
                                   boolean requireLocked, boolean requireMatching) {
        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return remaining;

        for (Long posLong : connected.getConnectedDrawers()) {
            if (remaining <= 0) break;
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));

            if (te instanceof WoodDrawerTile) {
                remaining = injectIntoBigHandler(((WoodDrawerTile) te).getHandler(),
                        (ControllableDrawerTile) te, inputStack, remaining, type, requireLocked, requireMatching);
            } else if (te instanceof CompactingDrawerTile) {
                remaining = injectIntoCompacting(((CompactingDrawerTile) te).getCompactingHandler(),
                        inputStack, remaining, type, requireLocked, requireMatching);
            }
        }
        return remaining;
    }

    private long injectIntoBigHandler(BigInventoryHandler handler, ControllableDrawerTile tile,
                                      ItemStack inputStack, long remaining, Actionable type,
                                      boolean requireLocked, boolean requireMatching) {
        if (requireLocked && !handler.isLocked()) return remaining;
        if (!requireLocked && handler.isLocked()) return remaining;

        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);

            if (requireMatching) {
                if (bs.getStack().isEmpty()) continue;
                if (!ItemUtil.areItemStacksEqual(bs.getStack(), inputStack)) continue;
            } else {
                if (!bs.getStack().isEmpty()) continue;
                if (handler.isLocked()) continue;
            }

            if (handler.isCreative()) {
                if (bs.getStack().isEmpty()) {
                    if (type == Actionable.MODULATE) {
                        ItemStack template = inputStack.copy();
                        template.setCount(inputStack.getMaxStackSize());
                        bs.setStack(template);
                        bs.setAmount(Long.MAX_VALUE);
                        handler.onChange();
                    }
                } else if (type == Actionable.MODULATE) {
                    bs.setAmount(Long.MAX_VALUE);
                    handler.onChange();
                }
                return 0;
            }

            long limit = handler.getLongSlotLimit(i);
            long space = requireMatching ? (limit - bs.getAmount()) : limit;
            long inserting = Math.min(remaining, space);
            if (inserting > 0) {
                if (type == Actionable.MODULATE) {
                    if (bs.getStack().isEmpty()) {
                        ItemStack template = inputStack.copy();
                        template.setCount(inputStack.getMaxStackSize());
                        bs.setStack(template);
                    }
                    bs.setAmount(bs.getAmount() + inserting);
                    handler.onChange();
                }
                remaining -= inserting;
            }

            // Void absorbs remaining for matching items
            if (handler.isVoid() && requireMatching && remaining > 0) {
                return 0;
            }

            if (remaining <= 0) return 0;
        }
        return remaining;
    }

    private long injectIntoCompacting(CompactingInventoryHandler handler, ItemStack inputStack,
                                      long remaining, Actionable type,
                                      boolean requireLocked, boolean requireMatching) {
        if (!handler.isSetup()) return remaining;
        if (requireLocked && !handler.isLocked()) return remaining;
        if (!requireLocked && handler.isLocked()) return remaining;
        if (!requireMatching) return remaining; // Compacting drawers don't have empty slots

        for (CompactingInventoryHandler.Result result : handler.getResults()) {
            if (result.getStack().isEmpty()) continue;
            if (!ItemUtil.areItemStacksEqual(result.getStack(), inputStack)) continue;

            long baseEquiv = remaining * result.getNeeded();
            long maxBase = handler.getTotalCapacity();
            long currentBase = handler.getTotalInBase();
            long canInsertBase = maxBase - currentBase;
            if (canInsertBase <= 0) {
                if (handler.isVoid()) return 0;
                return remaining;
            }

            long insertedBase = Math.min(baseEquiv, canInsertBase);
            long insertedItems = insertedBase / result.getNeeded();
            if (insertedItems <= 0) {
                if (handler.isVoid()) return 0;
                return remaining;
            }

            if (type == Actionable.MODULATE && !handler.isCreative()) {
                handler.setTotalInBase(currentBase + insertedItems * result.getNeeded());
                handler.onChange();
            }

            remaining -= insertedItems;
            if (handler.isVoid() && remaining > 0) return 0;
            return Math.max(0, remaining);
        }
        return remaining;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;

        ItemStack requestStack = request.getDefinition();
        long toExtract = request.getStackSize();
        long extracted = 0;

        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return null;

        for (Long posLong : connected.getConnectedDrawers()) {
            if (extracted >= toExtract) break;
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));

            if (te instanceof WoodDrawerTile) {
                extracted += extractFromBigHandler(((WoodDrawerTile) te).getHandler(),
                        requestStack, toExtract - extracted, mode);
            } else if (te instanceof CompactingDrawerTile) {
                extracted += extractFromCompacting(((CompactingDrawerTile) te).getCompactingHandler(),
                        requestStack, toExtract - extracted, mode);
            }
        }

        if (extracted <= 0) return null;

        IAEItemStack result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    private long extractFromBigHandler(BigInventoryHandler handler, ItemStack requestStack,
                                       long amount, Actionable mode) {
        long extracted = 0;
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (bs.getStack().isEmpty() || !ItemUtil.areItemStacksEqual(bs.getStack(), requestStack)) continue;

            long available = handler.isCreative() ? amount : bs.getAmount();
            long extracting = Math.min(amount - extracted, available);
            if (extracting > 0) {
                if (mode == Actionable.MODULATE && !handler.isCreative()) {
                    bs.setAmount(bs.getAmount() - extracting);
                    if (bs.getAmount() <= 0 && !handler.isLocked()) {
                        bs.setStack(ItemStack.EMPTY);
                    }
                    handler.onChange();
                }
                extracted += extracting;
            }
        }
        return extracted;
    }

    private long extractFromCompacting(CompactingInventoryHandler handler, ItemStack requestStack,
                                       long amount, Actionable mode) {
        if (!handler.isSetup()) return 0;
        for (CompactingInventoryHandler.Result result : handler.getResults()) {
            if (result.getStack().isEmpty()) continue;
            if (!ItemUtil.areItemStacksEqual(result.getStack(), requestStack)) continue;

            long totalBase = handler.getTotalInBase();
            long available = handler.isCreative() ? amount : totalBase / result.getNeeded();
            long extracting = Math.min(amount, available);
            if (extracting > 0) {
                if (mode == Actionable.MODULATE && !handler.isCreative()) {
                    handler.setTotalInBase(totalBase - extracting * result.getNeeded());
                    handler.onChange();
                }
                return extracting;
            }
        }
        return 0;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return out;

        for (Long posLong : connected.getConnectedDrawers()) {
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));

            if (te instanceof WoodDrawerTile) {
                BigInventoryHandler handler = ((WoodDrawerTile) te).getHandler();
                addBigHandlerItems(handler, out);
            } else if (te instanceof CompactingDrawerTile) {
                CompactingInventoryHandler handler = ((CompactingDrawerTile) te).getCompactingHandler();
                addCompactingItems(handler, out);
            }
        }
        return out;
    }

    private void addBigHandlerItems(BigInventoryHandler handler, IItemList<IAEItemStack> out) {
        for (int i = 0; i < handler.getSlotCount(); i++) {
            BigStack bs = handler.getStoredStacks().get(i);
            if (!bs.getStack().isEmpty() && bs.getAmount() > 0) {
                IAEItemStack aeStack = channel.createStack(bs.getStack());
                if (aeStack != null) {
                    aeStack.setStackSize(handler.isCreative() ? Long.MAX_VALUE : bs.getAmount());
                    out.addStorage(aeStack);
                }
            }
        }
    }

    private void addCompactingItems(CompactingInventoryHandler handler, IItemList<IAEItemStack> out) {
        if (!handler.isSetup()) return;
        long totalBase = handler.getTotalInBase();
        for (CompactingInventoryHandler.Result result : handler.getResults()) {
            if (result.getStack().isEmpty()) continue;
            long amount = handler.isCreative() ? Long.MAX_VALUE : totalBase / result.getNeeded();
            if (amount <= 0) continue;
            IAEItemStack aeStack = channel.createStack(result.getStack());
            if (aeStack != null) {
                aeStack.setStackSize(amount);
                out.addStorage(aeStack);
            }
        }
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
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null) return false;
        ItemStack inputStack = input.getDefinition();

        ConnectedDrawers connected = controller.getConnectedDrawers();
        if (connected == null) return false;

        for (Long posLong : connected.getConnectedDrawers()) {
            TileEntity te = controller.getWorld().getTileEntity(BlockPos.fromLong(posLong));

            if (te instanceof WoodDrawerTile) {
                BigInventoryHandler handler = ((WoodDrawerTile) te).getHandler();
                for (int i = 0; i < handler.getSlotCount(); i++) {
                    BigStack bs = handler.getStoredStacks().get(i);
                    if (bs.getStack().isEmpty() && !handler.isLocked()) return true;
                    if (!bs.getStack().isEmpty() && ItemUtil.areItemStacksEqual(bs.getStack(), inputStack)) {
                        if (bs.getAmount() < handler.getLongSlotLimit(i) || handler.isVoid()) return true;
                    }
                }
            } else if (te instanceof CompactingDrawerTile) {
                CompactingInventoryHandler handler = ((CompactingDrawerTile) te).getCompactingHandler();
                if (handler.isSetup()) {
                    for (CompactingInventoryHandler.Result result : handler.getResults()) {
                        if (!result.getStack().isEmpty() && ItemUtil.areItemStacksEqual(result.getStack(), inputStack)) {
                            if (handler.getTotalInBase() < handler.getTotalCapacity() || handler.isVoid()) return true;
                        }
                    }
                }
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
