package com.xinyihl.functionalstoragelegacy.misc;

import com.xinyihl.functionalstoragelegacy.FunctionalStorageLegacy;
import com.xinyihl.functionalstoragelegacy.Tags;
import com.xinyihl.functionalstoragelegacy.client.render.ControllerRenderer;
import com.xinyihl.functionalstoragelegacy.client.render.DrawerRenderer;
import com.xinyihl.functionalstoragelegacy.common.block.WoodDrawerBlock;
import com.xinyihl.functionalstoragelegacy.common.tile.EnderDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.FluidDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.WoodDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.CompactingDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.compact.SimpleCompactingDrawerTile;
import com.xinyihl.functionalstoragelegacy.common.tile.controller.DrawerControllerTile;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // Register TESRs
        ClientRegistry.bindTileEntitySpecialRenderer(WoodDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(CompactingDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(SimpleCompactingDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(FluidDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(EnderDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(DrawerControllerTile.class, new ControllerRenderer());
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MOD_ID)
    public static class ModelRegistration {

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            // Wood drawer blocks
            for (WoodDrawerBlock block : FunctionalStorageLegacy.WOOD_DRAWER_BLOCKS) {
                registerBlockModel(block);
            }

            // Special blocks
            registerBlockModel(FunctionalStorageLegacy.DRAWER_CONTROLLER_BLOCK);
            registerBlockModel(FunctionalStorageLegacy.CONTROLLER_EXTENSION_BLOCK);
            registerBlockModel(FunctionalStorageLegacy.COMPACTING_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLegacy.SIMPLE_COMPACTING_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLegacy.FLUID_DRAWER_1);
            registerBlockModel(FunctionalStorageLegacy.FLUID_DRAWER_2);
            registerBlockModel(FunctionalStorageLegacy.FLUID_DRAWER_4);
            registerBlockModel(FunctionalStorageLegacy.ENDER_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLegacy.ARMORY_CABINET_BLOCK);

            // Items
            registerItemModel(FunctionalStorageLegacy.IRON_DOWNGRADE);
            registerItemModel(FunctionalStorageLegacy.COPPER_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.GOLD_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.DIAMOND_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.NETHERITE_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.CREATIVE_VENDING_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.VOID_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.REDSTONE_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.PULLING_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.PUSHING_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.COLLECTOR_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.WIRELESS_PULLING_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.WIRELESS_PUSHING_UPGRADE);
            registerItemModel(FunctionalStorageLegacy.CONFIGURATION_TOOL);
            registerItemModel(FunctionalStorageLegacy.LINKING_TOOL);
            registerItemModel(FunctionalStorageLegacy.STONE_GENERATION_UPGRADE_BASIC);
            registerItemModel(FunctionalStorageLegacy.STONE_GENERATION_UPGRADE_ADVANCED);
            registerItemModel(FunctionalStorageLegacy.STONE_GENERATION_UPGRADE_REINFORCED);
            registerItemModel(FunctionalStorageLegacy.STONE_GENERATION_UPGRADE_MAGICAL);
        }

        private static void registerBlockModel(Block block) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(Objects.requireNonNull(block.getRegistryName()), "inventory"));
        }

        private static void registerItemModel(Item item) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
        }
    }
}
