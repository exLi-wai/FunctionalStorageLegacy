package com.xinyihl.functionalstoragelegacy.misc;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Mod configuration. Mirrors FunctionalStorageConfig from 1.21 version.
 */
public class FunctionalStorageConfig {

    public static int ARMORY_CABINET_SIZE = 4096;
    public static int DRAWER_CONTROLLER_LINKING_RANGE = 8;
    public static int UPGRADE_TICK = 4;
    public static int UPGRADE_PULL_ITEMS = 4;
    public static int UPGRADE_PULL_FLUID = 500;
    public static int UPGRADE_PUSH_ITEMS = 4;
    public static int UPGRADE_PUSH_FLUID = 500;
    public static int UPGRADE_COLLECTOR_ITEMS = 4;
    public static int UPGRADE_COLLECTOR_FLUID = 500;
    public static int COPPER_MULTIPLIER = 8;
    public static int GOLD_MULTIPLIER = 16;
    public static int DIAMOND_MULTIPLIER = 24;
    public static int NETHERITE_MULTIPLIER = 32;
    public static int FLUID_DIVISOR = 2;
    public static int RANGE_DIVISOR = 4;
    public static float STONE_GENERATION_T1= 2.0f;
    public static float STONE_GENERATION_T2= 4.0f;
    public static float STONE_GENERATION_T3= 8.0f;
    public static float STONE_GENERATION_T4= 16.0f;
    public static boolean ENABLE_TOP_COMPATIBILITY = true;

    public static void init(File configFile) {
        Configuration config = new Configuration(configFile);
        config.load();

        ARMORY_CABINET_SIZE = config.getInt("armoryCabinetSize", "general", 4096, 1, Integer.MAX_VALUE, "Armory slot amount");
        DRAWER_CONTROLLER_LINKING_RANGE = config.getInt("drawerControllerLinkingRange", "general", 8, 1, 64, "Linking range radius");
        UPGRADE_TICK = config.getInt("upgradeTick", "general", 4, 1, 200, "Every how many ticks the drawer upgrades will work");
        UPGRADE_PULL_ITEMS = config.getInt("upgradePullItems", "general", 4, 1, 64, "How many items the pulling upgrade will try to pull");
        UPGRADE_PULL_FLUID = config.getInt("upgradePullFluid", "general", 500, 1, 10000, "How much fluid (in mb) the pulling upgrade will try to pull");
        UPGRADE_PUSH_ITEMS = config.getInt("upgradePushItems", "general", 4, 1, 64, "How many items the pushing upgrade will try to push");
        UPGRADE_PUSH_FLUID = config.getInt("upgradePushFluid", "general", 500, 1, 10000, "How much fluid (in mb) the pushing upgrade will try to push");
        UPGRADE_COLLECTOR_ITEMS = config.getInt("upgradeCollectorItems", "general", 4, 1, 64, "How many items the collector upgrade will try to pull");
        UPGRADE_COLLECTOR_FLUID = config.getInt("upgradeCollectorFluid", "general", 500, 1, 10000, "How much fluid (in mb) the collector upgrade will try to collect");
        ENABLE_TOP_COMPATIBILITY = config.getBoolean("enableTOPCompatibility", "compatibility", true, "Enable The One Probe compatibility integration");

        COPPER_MULTIPLIER = config.getInt("copperMultiplier", "storage", 8, 1, 1024, "Copper Upgrade storage multiplier");
        GOLD_MULTIPLIER = config.getInt("goldMultiplier", "storage", 16, 1, 1024, "Gold Upgrade storage multiplier");
        DIAMOND_MULTIPLIER = config.getInt("diamondMultiplier", "storage", 24, 1, 1024, "Diamond Upgrade storage multiplier");
        NETHERITE_MULTIPLIER = config.getInt("netheriteMultiplier", "storage", 32, 1, 1024, "Netherite Upgrade storage multiplier");
        FLUID_DIVISOR = config.getInt("fluidDivisor", "storage", 2, 1, 64, "Fluid storage divisor for Storage Upgrades");
        RANGE_DIVISOR = config.getInt("rangeDivisor", "storage", 4, 1, 64, "Range divisor for Storage Upgrades");

        STONE_GENERATION_T1 = config.getFloat("stoneGenerationT1", "generation", 2.0f, 1.0f, Float.MAX_VALUE, "Stone Generation Upgrade T1 generation rate");
        STONE_GENERATION_T2 = config.getFloat("stoneGenerationT2", "generation", 4.0f, 1.0f, Float.MAX_VALUE, "Stone Generation Upgrade T2 generation rate");
        STONE_GENERATION_T3 = config.getFloat("stoneGenerationT3", "generation", 8.0f, 1.0f, Float.MAX_VALUE, "Stone Generation Upgrade T3 generation rate");
        STONE_GENERATION_T4 = config.getFloat("stoneGenerationT4", "generation", 16.0f, 1.0f, Float.MAX_VALUE, "Stone Generation Upgrade T4 generation rate");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
