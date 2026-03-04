package com.xinyihl.functionalstoragelgeacy.client;

import com.xinyihl.functionalstoragelgeacy.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID + "-client", category = "client")
@Config.LangKey("config.functionalstoragelgeacy.client")
public class FunctionalStorageClientConfig {

    @Config.Comment("Drawer content render range in blocks (default: 16)")
    @Config.RangeInt(min = 1, max = 128)
    public static int DRAWER_RENDER_RANGE = 16;

    @Config.Comment("The thickness of 3D item/block displays on drawer faces (default: 0.125)")
    @Config.RangeDouble(min = 0.05, max = 0.75)
    public static double DRAWER_RENDER_THICKNESS = 0.125;

    @Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Tags.MOD_ID)) {
                ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
