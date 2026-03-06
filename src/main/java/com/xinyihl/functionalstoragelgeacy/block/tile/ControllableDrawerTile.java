package com.xinyihl.functionalstoragelgeacy.block.tile;

import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import com.xinyihl.functionalstoragelgeacy.item.ConfigurationToolItem;
import com.xinyihl.functionalstoragelgeacy.item.StorageUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.item.UpgradeItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.HashMap;

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
    private boolean needsUpgradeCache = true;
    private float storageMultiplier = 1;
    private float fluidMultiplier = 1;
    private float rangeMultiplier = 1;
    private boolean hasIronDowngrade = false;

    public ControllableDrawerTile() {
        this.drawerOptions = new DrawerOptions();
        this.storageUpgrades = new ItemStackHandler(getStorageUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof StorageUpgradeItem
                        || stack.getItem() == FunctionalStorageLgeacy.CREATIVE_VENDING_UPGRADE;
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
        this.utilityUpgrades = new ItemStackHandler(getUtilityUpgradesAmount()) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof UpgradeItem
                        && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.UTILITY;
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
            if (!stack.isEmpty() && stack.getItem() instanceof UpgradeItem) {
                ((UpgradeItem) stack.getItem()).onTick(this, stack, i);
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
                || heldStack.getItem() == FunctionalStorageLgeacy.LINKING_TOOL) {
            return false;
        }

        // Try to insert storage upgrade
        if (heldStack.getItem() instanceof StorageUpgradeItem
                || heldStack.getItem() == FunctionalStorageLgeacy.CREATIVE_VENDING_UPGRADE) {
            for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                if (storageUpgrades.getStackInSlot(i).isEmpty() && storageUpgrades.isItemValid(i, heldStack)) {
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
                        if (newUpgrade.getTier().getMultiplier() > existingUpgrade.getTier().getMultiplier()) {
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
        if (heldStack.getItem() instanceof UpgradeItem
                && ((UpgradeItem) heldStack.getItem()).getType() == UpgradeItem.Type.UTILITY) {
            for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
                if (utilityUpgrades.getStackInSlot(i).isEmpty() && utilityUpgrades.isItemValid(i, heldStack)) {
                    ItemStack toInsert = heldStack.splitStack(1);
                    utilityUpgrades.setStackInSlot(i, toInsert);
                    return true;
                }
            }
        }

        // Open GUI if no slot hit
        if (slot == -1) {
            player.openGui(FunctionalStorageLgeacy.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
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
        isCreative = false;
        
        float mult = 1.0f;
        float fluidMult = 1.0f;
        float rangeMult = 1.0f;
        boolean iron = false;

        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
            ItemStack stack = storageUpgrades.getStackInSlot(i);
            if (stack.getItem() instanceof StorageUpgradeItem) {
                StorageUpgradeItem upgrade = (StorageUpgradeItem) stack.getItem();
                if (upgrade.getTier() == StorageUpgradeItem.StorageTier.IRON) {
                    iron = true;
                } else {
                    float tierMult = upgrade.getTier().getMultiplier();
                    mult *= tierMult;
                    fluidMult *= (tierMult / FunctionalStorageConfig.FLUID_DIVISOR);
                    rangeMult *= (tierMult / FunctionalStorageConfig.RANGE_DIVISOR);
                }
            }
            if (stack.getItem() == FunctionalStorageLgeacy.CREATIVE_VENDING_UPGRADE) {
                isCreative = true;
            }
        }

        isVoid = false;
        for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
            if (utilityUpgrades.getStackInSlot(i).getItem() == FunctionalStorageLgeacy.VOID_UPGRADE) {
                isVoid = true;
            }
        }

        this.storageMultiplier = Math.max(mult, 0f);
        this.fluidMultiplier = Math.max(fluidMult, 0f);
        this.rangeMultiplier = Math.max(rangeMult, 0f);
        this.hasIronDowngrade = iron;
        
        needsUpgradeCache = false;
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
            if (stack.getItem() == FunctionalStorageLgeacy.REDSTONE_UPGRADE) {
                return calculateRedstoneSignal();
            }
        }
        return 0;
    }

    protected int calculateRedstoneSignal() {
        return 0; // Override in subclasses
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
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(@Nonnull net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    /**
     * Get the item handler for this drawer (for capability).
     */
    public abstract IItemHandler getItemHandler();

    /**
     * Drawer options for rendering configuration.
     */
    public static class DrawerOptions {
        private final HashMap<ConfigurationToolItem.ConfigurationAction, Boolean> options;
        private final HashMap<ConfigurationToolItem.ConfigurationAction, Integer> advancedOptions;

        public DrawerOptions() {
            this.options = new HashMap<>();
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS, true);
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER, true);
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_UPGRADES, true);
            this.advancedOptions = new HashMap<>();
            this.advancedOptions.put(ConfigurationToolItem.ConfigurationAction.INDICATOR, 0);
        }

        public boolean isActive(ConfigurationToolItem.ConfigurationAction action) {
            return options.getOrDefault(action, true);
        }

        public boolean isShowItemRender() {
            return isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER);
        }

        public boolean isShowItemCount() {
            return isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS);
        }

        public void setActive(ConfigurationToolItem.ConfigurationAction action, boolean active) {
            options.put(action, active);
        }

        public int getAdvancedValue(ConfigurationToolItem.ConfigurationAction action) {
            return advancedOptions.getOrDefault(action, 0);
        }

        public void setAdvancedValue(ConfigurationToolItem.ConfigurationAction action, int value) {
            advancedOptions.put(action, value);
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            for (ConfigurationToolItem.ConfigurationAction action : options.keySet()) {
                nbt.setBoolean(action.name(), options.get(action));
            }
            for (ConfigurationToolItem.ConfigurationAction action : advancedOptions.keySet()) {
                nbt.setInteger("Advanced_" + action.name(), advancedOptions.get(action));
            }
            return nbt;
        }

        public void deserializeNBT(NBTTagCompound nbt) {
            for (String key : nbt.getKeySet()) {
                if (key.startsWith("Advanced_")) {
                    String actionName = key.substring("Advanced_".length());
                    try {
                        ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.ConfigurationAction.valueOf(actionName);
                        advancedOptions.put(action, nbt.getInteger(key));
                    } catch (IllegalArgumentException ignored) {}
                } else {
                    try {
                        ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.ConfigurationAction.valueOf(key);
                        options.put(action, nbt.getBoolean(key));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }
}
