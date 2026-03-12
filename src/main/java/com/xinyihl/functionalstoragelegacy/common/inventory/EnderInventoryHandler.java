package com.xinyihl.functionalstoragelegacy.common.inventory;

import com.xinyihl.functionalstoragelegacy.common.inventory.base.BigInventoryHandler;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Ender inventory handler - extends BigInventoryHandler with a frequency UUID for cross-dimensional sharing.
 */
public abstract class EnderInventoryHandler extends BigInventoryHandler {

    private String frequency = "";
    private boolean locked = false;
    private boolean isVoid = false;
    private boolean isCreative = false;
    private float multiplier = 1;
    public boolean needUpdate = false;

    public EnderInventoryHandler() {
        super(1); // Ender drawer has 1 slot, base 32 slot amount
    }

    @Override
    public void onChange() {
        needUpdate = true;
    }

    public boolean needUpdate() {
        return needUpdate;
    }

    @Override
    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isVoid() {
        return isVoid;
    }

    public void setVoid(boolean isVoid) {
        this.isVoid = isVoid;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isCreative() {
        return isCreative;
    }

    public void setCreative(boolean isCreative) {
        this.isCreative = isCreative;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public NBTTagCompound serializeNBTFull() {
        NBTTagCompound nbt = serializeNBT();
        nbt.setString("Frequency", frequency);
        nbt.setBoolean("Locked", locked);
        nbt.setBoolean("IsVoid", isVoid);
        nbt.setBoolean("IsCreative", isCreative);
        nbt.setFloat("Multiplier", multiplier);
        return nbt;
    }

    public void deserializeNBTFull(NBTTagCompound nbt) {
        deserializeNBT(nbt);
        frequency = nbt.getString("Frequency");
        locked = nbt.getBoolean("Locked");
        isVoid = nbt.getBoolean("IsVoid");
        isCreative = nbt.getBoolean("IsCreative");
        multiplier = nbt.getFloat("Multiplier");
        if (multiplier == 0) multiplier = 1;
    }
}
