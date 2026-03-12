package com.xinyihl.functionalstoragelegacy.client.render;

import com.xinyihl.functionalstoragelegacy.common.item.ConfigurationToolItem;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;

/**
 * Drawer options for rendering configuration.
 */
public class DrawerOptions {
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
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                try {
                    ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.ConfigurationAction.valueOf(key);
                    options.put(action, nbt.getBoolean(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
