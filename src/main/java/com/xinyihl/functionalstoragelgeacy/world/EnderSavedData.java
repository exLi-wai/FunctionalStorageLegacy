package com.xinyihl.functionalstoragelgeacy.world;

import com.xinyihl.functionalstoragelgeacy.inventory.EnderInventoryHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * World-saved data for ender drawer frequencies.
 * Holds a map of frequency UUID strings to EnderInventoryHandler instances.
 * All ender drawers sharing a frequency share the same inventory.
 */
public class EnderSavedData extends WorldSavedData {

    private static final String DATA_NAME = "functionalstoragelgeacy_ender";

    private final Map<String, EnderInventoryHandler> frequencyMap = new HashMap<>();

    public EnderSavedData() {
        super(DATA_NAME);
    }

    public EnderSavedData(String name) {
        super(name);
    }

    public static EnderSavedData getInstance(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            // Fallback - shouldn't happen normally
            return new EnderSavedData();
        }
        EnderSavedData instance = (EnderSavedData) storage.getOrLoadData(EnderSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new EnderSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    public EnderInventoryHandler getFrequency(String frequency) {
        return frequencyMap.computeIfAbsent(frequency, f -> {
            EnderInventoryHandler handler = new EnderInventoryHandler() {
                @Override
                public void onChange() {
                    markDirty();
                }
            };
            handler.setFrequency(frequency);
            return handler;
        });
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        frequencyMap.clear();
        int count = nbt.getInteger("FrequencyCount");
        for (int i = 0; i < count; i++) {
            String key = nbt.getString("Freq_" + i);
            NBTTagCompound data = nbt.getCompoundTag("FreqData_" + i);
            EnderInventoryHandler handler = new EnderInventoryHandler() {
                @Override
                public void onChange() {
                    markDirty();
                }
            };
            handler.setFrequency(key);
            handler.deserializeNBTFull(data);
            frequencyMap.put(key, handler);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        int i = 0;
        for (Map.Entry<String, EnderInventoryHandler> entry : frequencyMap.entrySet()) {
            nbt.setString("Freq_" + i, entry.getKey());
            nbt.setTag("FreqData_" + i, entry.getValue().serializeNBTFull());
            i++;
        }
        nbt.setInteger("FrequencyCount", i);
        return nbt;
    }
}
