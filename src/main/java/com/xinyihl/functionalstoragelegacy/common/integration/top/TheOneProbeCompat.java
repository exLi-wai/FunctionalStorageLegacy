package com.xinyihl.functionalstoragelegacy.common.integration.top;

import com.xinyihl.functionalstoragelegacy.misc.FunctionalStorageConfig;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class TheOneProbeCompat {

    public static void register() {
        if (FunctionalStorageConfig.ENABLE_TOP_COMPATIBILITY && Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", TOPIntegration.class.getName());
        }
    }
}
