package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.misc.Configurations;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;

public class AE2Compat {

    private static boolean checked = false;
    private static boolean loaded = false;

    public static boolean isLoaded() {
        if (!checked) {
            checked = true;
            loaded = Configurations.COMPATIBILITY.enableAE2Compatibility && Loader.isModLoaded("appliedenergistics2");
        }
        return loaded;
    }

    public static boolean isStorageAccessorCapability(Capability<?> capability) {
        if (!isLoaded()) return false;
        return AE2CapabilityHelper.isStorageAccessor(capability);
    }

    public static Object createAccessor(ControllableDrawerTile tile) {
        return AE2CapabilityHelper.createAccessor(tile);
    }
}
