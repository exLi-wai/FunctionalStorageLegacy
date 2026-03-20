package com.xinyihl.functionalstoragelegacy.util;

import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.SimpleCompactingDrawerTile;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for finding compacting recipes (e.g. Iron Nugget -> Iron Ingot -> Iron Block).
 * Uses CraftingManager to look up 3x3, 2x2, and 1x1 recipes.
 */
public class CompactingUtil {

    /**
     * Find compacting results anchored to a clicked slot.
     * The clicked slot will always display the clicked item, slots to the left become higher tiers,
     * and slots to the right become lower tiers. Remaining slots are empty.
     *
     * @param world       The world instance for recipe lookup
     * @param stack       The item to find compacting recipes for
     * @param maxSlots    Maximum number of tiers (2 or 3)
     * @param clickedSlot The slot index that was clicked (0-based)
     * @return List of compacting tiers
     */
    public static List<CompactingInventoryHandler.Result> getCompactingResults(World world, ItemStack stack, int maxSlots, int clickedSlot) {
        List<CompactingInventoryHandler.Result> fallback = getCompactingResults(world, stack, maxSlots);
        if (clickedSlot < 0 || clickedSlot >= maxSlots || stack.isEmpty()) {
            return fallback;
        }

        int maxHigher = clickedSlot;
        int maxLower = maxSlots - clickedSlot - 1;

        List<HigherTier> higherTiers = new ArrayList<>();
        ItemStack searching = stack.copy();
        searching.setCount(1);
        for (int i = 0; i < maxHigher; i++) {
            HigherTier higher = findHigherTier(world, searching);
            if (higher == null) {
                break;
            }
            higherTiers.add(higher);
            searching = higher.result.copy();
        }

        List<LowerTier> lowerTiers = new ArrayList<>();
        searching = stack.copy();
        searching.setCount(1);
        for (int i = 0; i < maxLower; i++) {
            LowerTier lower = findLowerTier(world, searching);
            if (lower == null) {
                break;
            }
            lowerTiers.add(lower);
            searching = lower.result.copy();
        }

        List<CompactingInventoryHandler.Result> anchored = new ArrayList<>();
        for (int i = 0; i < maxSlots; i++) {
            anchored.add(new CompactingInventoryHandler.Result(ItemStack.EMPTY, 1));
        }

        int clickedNeeded = 1;
        for (LowerTier lower : lowerTiers) {
            clickedNeeded *= lower.count;
        }

        ItemStack clickedStack = stack.copy();
        clickedStack.setCount(1);
        anchored.set(clickedSlot, new CompactingInventoryHandler.Result(clickedStack, clickedNeeded));

        int higherNeeded = clickedNeeded;
        for (int i = 0; i < higherTiers.size(); i++) {
            HigherTier higher = higherTiers.get(i);
            higherNeeded *= higher.inputCount;
            int targetSlot = clickedSlot - 1 - i;
            if (targetSlot < 0) {
                break;
            }
            anchored.set(targetSlot, new CompactingInventoryHandler.Result(higher.result.copy(), higherNeeded));
        }

        int lowerNeeded = clickedNeeded;
        for (int i = 0; i < lowerTiers.size(); i++) {
            LowerTier lower = lowerTiers.get(i);
            lowerNeeded /= lower.count;
            int targetSlot = clickedSlot + 1 + i;
            if (targetSlot >= maxSlots) {
                break;
            }
            anchored.set(targetSlot, new CompactingInventoryHandler.Result(lower.result.copy(), lowerNeeded));
        }

        return anchored;
    }


    /**
     * Find compacting results for a given item.
     * Returns a list of Result from highest tier to lowest tier (base item last).
     */
    private static List<CompactingInventoryHandler.Result> getCompactingResults(World world, ItemStack stack, int maxSlots) {
        List<CompactingInventoryHandler.Result> results = new ArrayList<>();

        // Start with the given item
        ItemStack current = stack.copy();
        current.setCount(1);

        // Try to find higher tiers (compact up)
        List<HigherTier> higherTiers = new ArrayList<>();
        ItemStack searching = current.copy();
        for (int tier = 0; tier < maxSlots - 1; tier++) {
            HigherTier higher = findHigherTier(world, searching);
            if (higher != null) {
                higherTiers.add(higher);
                searching = higher.result.copy();
            } else {
                break;
            }
        }

        if (higherTiers.isEmpty()) {
            // Try to find lower tiers (decompress down)
            List<LowerTier> lowerTiers = new ArrayList<>();
            searching = current.copy();
            for (int tier = 0; tier < maxSlots - 1; tier++) {
                LowerTier lower = findLowerTier(world, searching);
                if (lower != null) {
                    lowerTiers.add(lower);
                    searching = lower.result.copy();
                } else {
                    break;
                }
            }

            // Build results from input (highest) down to lowest tier
            // needed = how many base (lowest tier) items equal one of this item
            // e.g., [Block(81), Ingot(9), Nugget(1)]
            int totalProduct = 1;
            for (LowerTier lt : lowerTiers) {
                totalProduct *= lt.count;
            }
            results.add(new CompactingInventoryHandler.Result(current, totalProduct));
            int divisor = 1;
            for (LowerTier lt : lowerTiers) {
                divisor *= lt.count;
                results.add(new CompactingInventoryHandler.Result(lt.result, totalProduct / divisor));
            }
        } else {
            // Build results from highest tier down to input
            // chain is [input, higher1, higher2], counts[i] = how many chain[i] to make chain[i+1]
            // needed: chain[0]=1, chain[1]=counts[0], chain[2]=counts[0]*counts[1], ...
            List<ItemStack> chain = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            chain.add(current);
            for (HigherTier ht : higherTiers) {
                counts.add(ht.inputCount);
                chain.add(ht.result);
            }

            // Calculate needed from bottom (input) up
            int[] neededArr = new int[chain.size()];
            neededArr[0] = 1;
            for (int i = 1; i < chain.size(); i++) {
                neededArr[i] = neededArr[i - 1] * counts.get(i - 1);
            }

            // Add results from highest to lowest: [highest(biggest needed), ..., input(1)]
            for (int i = chain.size() - 1; i >= 0; i--) {
                results.add(new CompactingInventoryHandler.Result(chain.get(i), neededArr[i]));
            }

            // Try to extend downward from input
            if (results.size() < maxSlots) {
                LowerTier lower = findLowerTier(world, current);
                if (lower != null) {
                    for (CompactingInventoryHandler.Result r : results) {
                        r.setNeeded(r.getNeeded() * lower.count);
                    }
                    results.add(new CompactingInventoryHandler.Result(lower.result, 1));
                }
            }
        }

        while (results.size() < maxSlots) {
            results.add(new CompactingInventoryHandler.Result(ItemStack.EMPTY, 1));
        }
        if (results.size() > maxSlots) {
            results = results.subList(0, maxSlots);
        }

        return results;
    }

    private static HigherTier findHigherTier(World world, ItemStack input) {
        HigherTier result = tryCompact(world, input, 3);
        if (result != null) return result;
        return tryCompact(world, input, 2);
    }

    private static LowerTier findLowerTier(World world, ItemStack input) {
        FakeContainer container = new FakeContainer(1);
        container.setInventorySlotContents(0, input.copy());
        IRecipe recipe = CraftingManager.findMatchingRecipe(container, world);
        if (recipe != null) {
            ItemStack output = recipe.getRecipeOutput();
            if (!output.isEmpty() && !ItemUtil.areItemStacksEqual(output, input) && output.getCount() > 1) {
                return new LowerTier(output.copy(), output.getCount());
            }
        }
        return null;
    }

    private static HigherTier tryCompact(World world, ItemStack input, int gridSize) {
        FakeContainer container = new FakeContainer(gridSize);
        for (int i = 0; i < gridSize * gridSize; i++) {
            container.setInventorySlotContents(i, input.copy());
        }
        IRecipe recipe = CraftingManager.findMatchingRecipe(container, world);
        if (recipe != null) {
            ItemStack output = recipe.getRecipeOutput();
            if (!output.isEmpty() && !ItemUtil.areItemStacksEqual(output, input)) {
                LowerTier reverse = findLowerTier(world, output);
                if (reverse != null && ItemUtil.areItemStacksEqual(reverse.result, input)) {
                    return new HigherTier(output.copy(), gridSize * gridSize);
                }
            }
        }
        return null;
    }

    public static boolean ItemRemainder(ControllableDrawerTile tile, IItemHandler handler, ItemStack itemToGenerate) {
        ItemStack remainder;

        if (tile instanceof SimpleCompactingDrawerTile) {
            remainder = handler.insertItem(1, itemToGenerate, false);
            if (!remainder.isEmpty()) {
                remainder = ItemHandlerHelper.insertItemStacked(handler, itemToGenerate, false);
            }
        } else {
            remainder = ItemHandlerHelper.insertItemStacked(handler, itemToGenerate, false);
        }

        return remainder.isEmpty();
    }

    public static boolean CompressionDrawertrEatment(ControllableDrawerTile tile, ItemStack itemToGenerate, CompactingInventoryHandler compactingHandler) {
        int anchorSlot = compactingHandler.getSlots() - 1;
        List<CompactingInventoryHandler.Result> results = CompactingUtil.getCompactingResults(
                tile.getWorld(),
                itemToGenerate,
                compactingHandler.getSlots(),
                anchorSlot
        );

        if (!results.isEmpty()) {
            while (results.size() < compactingHandler.getSlots()) {
                results.add(new CompactingInventoryHandler.Result(ItemStack.EMPTY, 1));
            }
            if (results.size() > compactingHandler.getSlots()) {
                results = results.subList(0, compactingHandler.getSlots());
            }
            compactingHandler.setResults(results);
        } else {
            return true;
        }
        return false;
    }

    private static class HigherTier {
        final ItemStack result;
        final int inputCount;

        HigherTier(ItemStack result, int inputCount) {
            this.result = result;
            this.result.setCount(1);
            this.inputCount = inputCount;
        }
    }

    private static class LowerTier {
        final ItemStack result;
        final int count;

        LowerTier(ItemStack result, int count) {
            this.result = result.copy();
            this.result.setCount(1);
            this.count = count;
        }
    }

    private static class FakeContainer extends InventoryCrafting {

        private final NonNullList<ItemStack> items;
        private final int size;

        public FakeContainer(int gridSize) {
            super(null, gridSize, gridSize);
            this.size = gridSize * gridSize;
            this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        }

        @Override
        public int getSizeInventory() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) return false;
            }
            return true;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int index) {
            if (index < 0 || index >= size) return ItemStack.EMPTY;
            return items.get(index);
        }

        @Override
        public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
            if (index >= 0 && index < size) {
                items.set(index, stack);
            }
        }

        @Nonnull
        @Override
        public ItemStack removeStackFromSlot(int index) {
            ItemStack stack = items.get(index);
            items.set(index, ItemStack.EMPTY);
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack decrStackSize(int index, int count) {
            ItemStack stack = items.get(index);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = stack.splitStack(count);
            if (stack.isEmpty()) items.set(index, ItemStack.EMPTY);
            return result;
        }
    }
}
