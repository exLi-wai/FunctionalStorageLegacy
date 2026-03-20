package com.xinyihl.functionalstoragelegacy.common.tile.base;

import com.xinyihl.functionalstoragelegacy.FunctionalStorageLegacy;
import com.xinyihl.functionalstoragelegacy.api.UpgradeState;
import com.xinyihl.functionalstoragelegacy.client.render.DrawerOptions;
import com.xinyihl.functionalstoragelegacy.common.item.ConfigurationToolItem;
import com.xinyihl.functionalstoragelegacy.common.item.upgrade.StorageUpgradeItem;
import com.xinyihl.functionalstoragelegacy.common.item.upgrade.UpgradeItem;
import com.xinyihl.functionalstoragelegacy.common.item.upgrade.UtilityUpgradeItem;
import com.xinyihl.functionalstoragelegacy.misc.Configurations;
import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import com.xinyihl.functionalstoragelegacy.util.ItemUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Abstract base TileEntity for all controllable drawer blocks.
 * Manages storage upgrades, utility upgrades, drawer options, and controller binding.
 */
public abstract class ControllableDrawerTile extends TileEntity implements ITickable {

    protected BlockPos controllerPos;
    protected ItemStackHandler storageUpgrades;
    protected ItemStackHandler utilityUpgrades;
    protected DrawerOptions drawerOptions;
    protected boolean isCreative = false;
    protected boolean isVoid = false;
    protected boolean isLocked = false;
    protected boolean needsUpgradeCache = true;
    private float storageMultiplier = 1;
    private float fluidMultiplier = 1;
    private float rangeMultiplier = 1;
    private boolean hasIronDowngrade = false;

    public ControllableDrawerTile() {
        this.drawerOptions = new DrawerOptions();
        this.storageUpgrades = new ItemStackHandler(getStorageUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return canInsertStorageUpgrade(slot, stack);
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!canRemoveStorageUpgrade(slot)) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                needsUpgradeCache = true;
                markDirty();
            }
        };
        this.utilityUpgrades = new ItemStackHandler(getUtilityUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return canInsertUtilityUpgrade(slot, stack);
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                needsUpgradeCache = true;
                markDirty();
            }
        };
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // Process utility upgrades
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            ItemStack stack = utilityUpgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof UtilityUpgradeItem) {
                ((UtilityUpgradeItem) stack.getItem()).onTick(this, stack, i);
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        compound.setTag("StorageUpgrades", storageUpgrades.serializeNBT());
        compound.setTag("UtilityUpgrades", utilityUpgrades.serializeNBT());
        compound.setTag("DrawerOptions", drawerOptions.serializeNBT());
        compound.setBoolean("IsCreative", isCreative);
        compound.setBoolean("IsVoid", isVoid);
        compound.setBoolean("Locked", isLocked);
        if (controllerPos != null) {
            compound.setLong("ControllerPos", controllerPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("StorageUpgrades")) {
            storageUpgrades.deserializeNBT(compound.getCompoundTag("StorageUpgrades"));
        }
        if (compound.hasKey("UtilityUpgrades")) {
            utilityUpgrades.deserializeNBT(compound.getCompoundTag("UtilityUpgrades"));
        }
        if (compound.hasKey("DrawerOptions")) {
            drawerOptions.deserializeNBT(compound.getCompoundTag("DrawerOptions"));
        }
        isCreative = compound.getBoolean("IsCreative");
        isVoid = compound.getBoolean("IsVoid");
        isLocked = compound.getBoolean("Locked");
        if (compound.hasKey("ControllerPos")) {
            controllerPos = BlockPos.fromLong(compound.getLong("ControllerPos"));
        }
        needsUpgradeCache = true;
    }

    /**
     * Save tile data for storing in item NBT.
     */
    public NBTTagCompound saveTileToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("StorageUpgrades", storageUpgrades.serializeNBT());
        nbt.setTag("UtilityUpgrades", utilityUpgrades.serializeNBT());
        nbt.setTag("DrawerOptions", drawerOptions.serializeNBT());
        nbt.setBoolean("IsCreative", isCreative);
        nbt.setBoolean("IsVoid", isVoid);
        nbt.setBoolean("Locked", isLocked);
        writeCustomData(nbt);
        return nbt;
    }

    /**
     * Load tile data from item NBT.
     */
    public void loadTileFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("StorageUpgrades")) {
            storageUpgrades.deserializeNBT(nbt.getCompoundTag("StorageUpgrades"));
        }
        if (nbt.hasKey("UtilityUpgrades")) {
            utilityUpgrades.deserializeNBT(nbt.getCompoundTag("UtilityUpgrades"));
        }
        if (nbt.hasKey("DrawerOptions")) {
            drawerOptions.deserializeNBT(nbt.getCompoundTag("DrawerOptions"));
        }
        isCreative = nbt.getBoolean("IsCreative");
        isVoid = nbt.getBoolean("IsVoid");
        isLocked = nbt.getBoolean("Locked");
        readCustomData(nbt);
        needsUpgradeCache = true;
        markDirty();
    }

    /**
     * Override in subclasses to save additional data (e.g. inventory contents).
     */
    protected abstract void writeCustomData(NBTTagCompound nbt);

    /**
     * Override in subclasses to load additional data.
     */
    protected abstract void readCustomData(NBTTagCompound nbt);

    /**
     * Handle right-click interaction on a specific slot.
     */
    public boolean onSlotActivated(EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ, int slot) {
        ItemStack heldStack = player.getHeldItem(hand);

        // Skip if using configuration or linking tool
        if (heldStack.getItem() instanceof ConfigurationToolItem
                || heldStack.getItem() == RegistrationHandler.LINKING_TOOL) {
            return false;
        }

        // Try to insert storage upgrade
        if (ItemUtil.isStorageUpgradeItem(heldStack)) {
            for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                if (storageUpgrades.getStackInSlot(i).isEmpty() && canInsertStorageUpgrade(i, heldStack)) {
                    ItemStack toInsert = heldStack.splitStack(1);
                    storageUpgrades.setStackInSlot(i, toInsert);
                    return true;
                }
            }
            // Try upgrading existing
            if (heldStack.getItem() instanceof StorageUpgradeItem) {
                StorageUpgradeItem newUpgrade = (StorageUpgradeItem) heldStack.getItem();
                for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                    ItemStack existing = storageUpgrades.getStackInSlot(i);
                    if (existing.getItem() instanceof StorageUpgradeItem) {
                        StorageUpgradeItem existingUpgrade = (StorageUpgradeItem) existing.getItem();
                        if (newUpgrade.getTier().getMultiplier() > existingUpgrade.getTier().getMultiplier()
                                && canReplaceStorageUpgrade(i, heldStack)) {
                            // Give back old upgrade
                            if (!player.inventory.addItemStackToInventory(existing.copy())) {
                                player.dropItem(existing.copy(), false);
                            }
                            ItemStack toInsert = heldStack.splitStack(1);
                            storageUpgrades.setStackInSlot(i, toInsert);
                            return true;
                        }
                    }
                }
            }
        }

        // Try to insert utility upgrade
        if (heldStack.getItem() instanceof UtilityUpgradeItem) {
            for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
                if (utilityUpgrades.getStackInSlot(i).isEmpty() && canInsertUtilityUpgrade(i, heldStack)) {
                    ItemStack toInsert = heldStack.splitStack(1);
                    utilityUpgrades.setStackInSlot(i, toInsert);
                    return true;
                }
            }
        }

        // Open GUI if no slot hit
        if (slot == -1) {
            player.openGui(FunctionalStorageLegacy.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }

        return false;
    }

    /**
     * Handle left-click extraction on a specific slot.
     */
    public void onClicked(EntityPlayer player, int slot) {
        // Override in subclasses
    }

    /**
     * Recalculate storage multiplier and special states from upgrades.
     */
    public void recalculateUpgrades() {
        UpgradeState state = calculateUpgradeState(null, ItemStack.EMPTY);
        isCreative = state.creative;
        isVoid = state.voidUpgrade;
        this.storageMultiplier = Math.max(state.storageMultiplier, 0f);
        this.fluidMultiplier = Math.max(state.fluidMultiplier, 0f);
        this.rangeMultiplier = Math.max(state.rangeMultiplier, 0f);
        this.hasIronDowngrade = state.ironDowngrade;

        needsUpgradeCache = false;
    }

    public boolean canInsertStorageUpgrade(int slot, @Nonnull ItemStack stack) {
        if (!ItemUtil.isStorageUpgradeItem(stack) || slot < 0 || slot >= storageUpgrades.getSlots()) {
            return false;
        }
        return !hasIncompatibleUpgrade(stack, slot);
    }

    public boolean canInsertUtilityUpgrade(int slot, @Nonnull ItemStack stack) {
        if (!ItemUtil.isUtilityUpgradeItem(stack) || slot < 0 || slot >= utilityUpgrades.getSlots()) {
            return false;
        }
        UtilityUpgradeItem utilityUpgrade = (UtilityUpgradeItem) stack.getItem();
        return utilityUpgrade.canInsertInto(this) && !hasIncompatibleUpgrade(stack, null);
    }

    public boolean canRemoveStorageUpgrade(int slot) {
        if (slot < 0 || slot >= storageUpgrades.getSlots()) {
            return false;
        }
        ItemStack existing = storageUpgrades.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return canApplyUpgradeState(calculateUpgradeState(slot, ItemStack.EMPTY));
    }

    public boolean canReplaceStorageUpgrade(int slot, @Nonnull ItemStack replacement) {
        if (!ItemUtil.isStorageUpgradeItem(replacement) || slot < 0 || slot >= storageUpgrades.getSlots()) {
            return false;
        }
        if (!storageUpgrades.getStackInSlot(slot).isEmpty() && hasIncompatibleUpgrade(replacement, slot)) {
            return false;
        }
        return canApplyUpgradeState(calculateUpgradeState(slot, replacement));
    }

    protected boolean canApplyUpgradeState(UpgradeState state) {
        return true;
    }

    protected UpgradeState calculateUpgradeState(@Nullable Integer replacedStorageSlot, @Nonnull ItemStack replacementStack) {
        UpgradeState state = new UpgradeState();

        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            ItemStack stack = storageUpgrades.getStackInSlot(i);
            if (replacedStorageSlot != null && replacedStorageSlot == i) {
                stack = replacementStack;
            }
            applyStorageUpgradeState(state, stack);
        }

        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            ItemStack stack = utilityUpgrades.getStackInSlot(i);
            if (stack.getItem() == RegistrationHandler.VOID_UPGRADE) {
                state.voidUpgrade = true;
            }
        }

        return state;
    }

    protected void applyStorageUpgradeState(UpgradeState state, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof StorageUpgradeItem) {
            StorageUpgradeItem upgrade = (StorageUpgradeItem) stack.getItem();
            if (upgrade.getTier() == StorageUpgradeItem.StorageTier.IRON) {
                state.ironDowngrade = true;
            } else {
                float tierMult = upgrade.getTier().getMultiplier();
                state.storageMultiplier *= tierMult;
                state.fluidMultiplier *= (tierMult / Configurations.STORAGE.fluidDivisor);
                state.rangeMultiplier += (tierMult / Configurations.STORAGE.rangeDivisor);
            }
        }
        if (stack.getItem() == RegistrationHandler.CREATIVE_VENDING_UPGRADE) {
            state.creative = true;
        }
    }

    protected boolean hasIncompatibleUpgrade(@Nonnull ItemStack candidate, @Nullable Integer ignoredStorageSlot) {
        Item candidateItem = candidate.getItem();
        Set<Item> candidateConflicts = getIncompatibleUpgrades(candidate);
        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            if (ignoredStorageSlot != null && ignoredStorageSlot == i) {
                continue;
            }
            ItemStack existing = storageUpgrades.getStackInSlot(i);
            if (isConflictingUpgrade(candidateItem, candidateConflicts, existing)) {
                return true;
            }
        }
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            ItemStack existing = utilityUpgrades.getStackInSlot(i);
            if (isConflictingUpgrade(candidateItem, candidateConflicts, existing)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isConflictingUpgrade(Item candidateItem, Set<Item> candidateConflicts, ItemStack existing) {
        if (existing.isEmpty()) {
            return false;
        }
        Item existingItem = existing.getItem();
        return candidateConflicts.contains(existingItem) || getIncompatibleUpgrades(existing).contains(candidateItem);
    }

    protected Set<Item> getIncompatibleUpgrades(@Nonnull ItemStack stack) {
        if (stack.getItem() instanceof UpgradeItem) {
            return ((UpgradeItem) stack.getItem()).getIncompatibleUpgrades(stack);
        }
        return Collections.emptySet();
    }

    public float getStorageMultiplier() {
        if (needsUpgradeCache) recalculateUpgrades();
        return storageMultiplier;
    }

    public float getFluidMultiplier() {
        if (needsUpgradeCache) recalculateUpgrades();
        return fluidMultiplier;
    }

    public float getRangeMultiplier() {
        if (needsUpgradeCache) recalculateUpgrades();
        return rangeMultiplier;
    }

    public boolean hasIronDowngrade() {
        if (needsUpgradeCache) recalculateUpgrades();
        return hasIronDowngrade;
    }

    public boolean isCreative() {
        if (needsUpgradeCache) recalculateUpgrades();
        return isCreative;
    }

    public boolean isVoid() {
        if (needsUpgradeCache) recalculateUpgrades();
        return isVoid;
    }

    public boolean isLocked() {
        if (needsUpgradeCache) recalculateUpgrades();
        return isLocked;
    }

    public void setLocked(boolean locked) {
        if (this.isLocked == locked) return;
        this.isLocked = locked;
        this.needsUpgradeCache = true;
        markDirty();
        sendUpdatePacket();
    }

    @Override
    public boolean shouldRefresh(@Nonnull World world, @Nonnull BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    public void toggleLocking() {
        setLocked(!isLocked());
    }

    public void toggleOption(ConfigurationToolItem.ConfigurationAction action) {
        if (action.getMax() == 1) {
            drawerOptions.setActive(action, !drawerOptions.isActive(action));
        } else {
            drawerOptions.setAdvancedValue(action, (drawerOptions.getAdvancedValue(action) + 1) % (action.getMax() + 1));
        }
        markDirty();
        sendUpdatePacket();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        markDirty();
    }

    public void clearControllerPos() {
        this.controllerPos = null;
        markDirty();
    }

    public ItemStackHandler getStorageUpgrades() {
        return storageUpgrades;
    }

    public ItemStackHandler getUtilityUpgrades() {
        return utilityUpgrades;
    }

    public DrawerOptions getDrawerOptions() {
        return drawerOptions;
    }

    public int getStorageUpgradesAmount() {
        return 4;
    }

    public int getUtilityUpgradesAmount() {
        return 3;
    }

    public boolean isEverythingEmpty() {
        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            if (!storageUpgrades.getStackInSlot(i).isEmpty()) return false;
        }
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            if (!utilityUpgrades.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public int getRedstoneSignal(EnumFacing side) {
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            ItemStack stack = utilityUpgrades.getStackInSlot(i);
            if (stack.getItem() == RegistrationHandler.REDSTONE_UPGRADE) {
                return calculateRedstoneSignal();
            }
        }
        return 0;
    }

    protected int calculateRedstoneSignal() {
        return 0;
    }

    public void sendUpdatePacket() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(@Nonnull NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    /**
     * Get the item handler for this drawer (for capability).
     */
    public abstract IItemHandler getItemHandler();

}
