package com.xinyihl.functionalstoragelegacy.api;

import com.xinyihl.functionalstoragelegacy.api.upgrade.ModifierType;
import com.xinyihl.functionalstoragelegacy.api.upgrade.UpgradeModifier;

import java.util.*;

public class UpgradeState {
    private final Map<ModifierType, List<UpgradeModifier>> modifiers = new EnumMap<>(ModifierType.class);
    public boolean maxStorage = false;
    public boolean creative = false;
    public boolean voidUpgrade = false;
    public boolean oreDictionary = false;

    public void addModifier(ModifierType type, UpgradeModifier modifier) {
        modifiers.computeIfAbsent(type, k -> new ArrayList<>()).add(modifier);
    }

    public void addModifiers(Map<ModifierType, UpgradeModifier> mods) {
        for (Map.Entry<ModifierType, UpgradeModifier> entry : mods.entrySet()) {
            addModifier(entry.getKey(), entry.getValue());
        }
    }

    public float calculate(ModifierType type, float defaultBase) {
        List<UpgradeModifier> mods = modifiers.get(type);
        if (mods == null || mods.isEmpty()) {
            return UpgradeModifier.calculate(defaultBase);
        }
        return UpgradeModifier.calculate(mods, defaultBase);
    }

    public List<UpgradeModifier> getModifiers(ModifierType type) {
        return modifiers.getOrDefault(type, Collections.emptyList());
    }
}
