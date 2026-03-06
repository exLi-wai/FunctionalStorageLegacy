package com.xinyihl.functionalstoragelgeacy.item;

import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.block.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Base class for drawer upgrade items.
 * Supports STORAGE and UTILITY types.
 * Utility upgrades can have tick-based behavior.
 */
public class UpgradeItem extends Item {

    public enum Type {
        STORAGE,
        UTILITY
    }

    public enum UtilityAction {
        NONE,
        PULLING,
        PUSHING,
        COLLECTOR,
        VOID,
        REDSTONE
    }

    private final Type type;
    private final UtilityAction utilityAction;

    public UpgradeItem(Type type) {
        this(type, UtilityAction.NONE);
    }

    public UpgradeItem(Type type, UtilityAction utilityAction) {
        this.type = type;
        this.utilityAction = utilityAction;
        this.setCreativeTab(FunctionalStorageLgeacy.CREATIVE_TAB);
    }

    public Type getType() {
        return type;
    }

    public UtilityAction getUtilityAction() {
        return utilityAction;
    }

    public boolean isDirectionalUtility() {
        return type == Type.UTILITY && (utilityAction == UtilityAction.PULLING
                || utilityAction == UtilityAction.PUSHING
                || utilityAction == UtilityAction.COLLECTOR);
    }

    /**
     * Get the direction stored on this upgrade's NBT tag.
     */
    public static EnumFacing getDirection(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Direction")) {
            return EnumFacing.byIndex(stack.getTagCompound().getInteger("Direction"));
        }
        return EnumFacing.NORTH;
    }

    /**
     * Set the direction on this upgrade's NBT tag.
     */
    public static void setDirection(ItemStack stack, EnumFacing facing) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Direction", facing.getIndex());
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World worldIn, EntityPlayer playerIn, @Nonnull EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (isDirectionalUtility() && playerIn.isSneaking()) {
            EnumFacing current = getDirection(stack);
            EnumFacing next = EnumFacing.byIndex((current.getIndex() + 1) % EnumFacing.values().length);
            setDirection(stack, next);

            if (!worldIn.isRemote) {
                playerIn.sendStatusMessage(new TextComponentTranslation(
                        "item.functionalstoragelgeacy.upgrade.direction.swapped",
                        new TextComponentTranslation("item.functionalstoragelgeacy.upgrade.direction." + next.getName())
                ).setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.GREEN)), true);
            }

            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@Nonnull ItemStack stack, World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (isDirectionalUtility()) {
            EnumFacing direction = getDirection(stack);
            tooltip.add(TextFormatting.YELLOW
                    + new TextComponentTranslation("item.functionalstoragelgeacy.upgrade.direction").getUnformattedText()
                    + " " + TextFormatting.AQUA
                    + new TextComponentTranslation("item.functionalstoragelgeacy.upgrade.direction." + direction.getName()).getUnformattedText());
            tooltip.add(TextFormatting.GRAY
                    + new TextComponentTranslation("item.functionalstoragelgeacy.upgrade.direction.use").getUnformattedText());
        }
    }

    /**
     * Called every tick for utility upgrades while installed in a drawer.
     */
    public void onTick(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot) {
        if (tile.getWorld() == null || tile.getWorld().isRemote) return;
        if (tile.getWorld().getTotalWorldTime() % FunctionalStorageConfig.UPGRADE_TICK != 0) return;

        switch (utilityAction) {
            case PULLING:
                handlePulling(tile, upgradeStack);
                break;
            case PUSHING:
                handlePushing(tile, upgradeStack);
                break;
            case COLLECTOR:
                handleCollector(tile, upgradeStack);
                break;
            default:
                break;
        }
    }

    private void handlePulling(ControllableDrawerTile tile, ItemStack upgradeStack) {
        EnumFacing dir = getDirection(upgradeStack);
        BlockPos neighborPos = tile.getPos().offset(dir);
        TileEntity neighborTE = tile.getWorld().getTileEntity(neighborPos);
        if (neighborTE == null) return;

        // Item pulling
        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler != null && neighborTE.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
            IItemHandler sourceHandler = neighborTE.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (sourceHandler != null) {
                for (int i = 0; i < sourceHandler.getSlots(); i++) {
                    ItemStack extracted = sourceHandler.extractItem(i, FunctionalStorageConfig.UPGRADE_PULL_ITEMS, true);
                    if (!extracted.isEmpty()) {
                        ItemStack remainder = ItemHandlerHelper.insertItemStacked(drawerHandler, extracted, true);
                        if (remainder.getCount() < extracted.getCount()) {
                            int toMove = extracted.getCount() - remainder.getCount();
                            ItemStack actualExtracted = sourceHandler.extractItem(i, toMove, false);
                            ItemHandlerHelper.insertItemStacked(drawerHandler, actualExtracted, false);
                            break;
                        }
                    }
                }
            }
        }

        // Fluid pulling
        if (tile instanceof FluidDrawerTile) {
            FluidDrawerTile fluidTile = (FluidDrawerTile) tile;
            if (neighborTE.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
                IFluidHandler sourceFluid = neighborTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
                if (sourceFluid != null) {
                    FluidStack drained = sourceFluid.drain(FunctionalStorageConfig.UPGRADE_PULL_FLUID, false);
                    if (drained != null && drained.amount > 0) {
                        int filled = fluidTile.getFluidHandler().fill(drained, true);
                        if (filled > 0) {
                            sourceFluid.drain(filled, true);
                        }
                    }
                }
            }
        }
    }

    private void handlePushing(ControllableDrawerTile tile, ItemStack upgradeStack) {
        EnumFacing dir = getDirection(upgradeStack);
        BlockPos neighborPos = tile.getPos().offset(dir);
        TileEntity neighborTE = tile.getWorld().getTileEntity(neighborPos);
        if (neighborTE == null) return;

        // Item pushing
        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler != null && neighborTE.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
            IItemHandler destHandler = neighborTE.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (destHandler != null) {
                for (int i = 0; i < drawerHandler.getSlots(); i++) {
                    ItemStack available = drawerHandler.extractItem(i, FunctionalStorageConfig.UPGRADE_PUSH_ITEMS, true);
                    if (!available.isEmpty()) {
                        ItemStack remainder = ItemHandlerHelper.insertItemStacked(destHandler, available, true);
                        if (remainder.getCount() < available.getCount()) {
                            int toMove = available.getCount() - remainder.getCount();
                            ItemStack extracted = drawerHandler.extractItem(i, toMove, false);
                            ItemHandlerHelper.insertItemStacked(destHandler, extracted, false);
                            break;
                        }
                    }
                }
            }
        }

        // Fluid pushing
        if (tile instanceof FluidDrawerTile) {
            FluidDrawerTile fluidTile = (FluidDrawerTile) tile;
            if (neighborTE.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
                IFluidHandler destFluid = neighborTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
                if (destFluid != null) {
                    FluidStack drained = fluidTile.getFluidHandler().drain(FunctionalStorageConfig.UPGRADE_PUSH_FLUID, false);
                    if (drained != null && drained.amount > 0) {
                        int filled = destFluid.fill(drained, true);
                        if (filled > 0) {
                            fluidTile.getFluidHandler().drain(filled, true);
                        }
                    }
                }
            }
        }
    }

    private void handleCollector(ControllableDrawerTile tile, ItemStack upgradeStack) {
        EnumFacing dir = getDirection(upgradeStack);
        BlockPos collectPos = tile.getPos().offset(dir);
        World world = tile.getWorld();

        IItemHandler drawerHandler = tile.getItemHandler();
        if (drawerHandler == null) return;

        // Use exact block AABB for collection area (matches 1.21 behavior)
        AxisAlignedBB collectArea = new AxisAlignedBB(collectPos);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, collectArea, EntitySelectors.IS_ALIVE);
        for (EntityItem entity : items) {
            if (entity.isDead) continue;
            ItemStack entityStack = entity.getItem();
            if (entityStack.isEmpty()) continue;

            // Limit items collected per entity to config value (matches 1.21 behavior)
            int maxCollect = Math.min(entityStack.getCount(), FunctionalStorageConfig.UPGRADE_COLLECTOR_ITEMS);
            ItemStack toInsert = entityStack.copy();
            toInsert.setCount(maxCollect);

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(drawerHandler, toInsert, false);
            int inserted = maxCollect - (remainder.isEmpty() ? 0 : remainder.getCount());
            if (inserted > 0) {
                entityStack.shrink(inserted);
                if (entityStack.isEmpty()) {
                    entity.setDead();
                } else {
                    entity.setItem(entityStack);
                }
                // Stop after processing one entity (matches 1.21 behavior)
                return;
            }
        }
    }
}
