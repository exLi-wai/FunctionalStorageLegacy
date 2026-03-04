package com.xinyihl.functionalstoragelgeacy.compat.top;

import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class TheOneProbeCompat {

    public static void register() {
        if (FunctionalStorageConfig.ENABLE_TOP_COMPATIBILITY && Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", TOPIntegration.class.getName());
        }
    }
}
