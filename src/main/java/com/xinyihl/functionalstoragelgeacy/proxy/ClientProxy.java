package com.xinyihl.functionalstoragelgeacy.proxy;

import com.xinyihl.functionalstoragelgeacy.FunctionalStorageLgeacy;
import com.xinyihl.functionalstoragelgeacy.Tags;
import com.xinyihl.functionalstoragelgeacy.block.WoodDrawerBlock;
import com.xinyihl.functionalstoragelgeacy.block.tile.*;
import com.xinyihl.functionalstoragelgeacy.client.ControllerRenderer;
import com.xinyihl.functionalstoragelgeacy.client.DrawerRenderer;
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
        ClientRegistry.bindTileEntitySpecialRenderer(DrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(CompactingDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(SimpleCompactingDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(FluidDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(EnderDrawerTile.class, new DrawerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(StorageControllerTile.class, new ControllerRenderer());
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MOD_ID)
    public static class ModelRegistration {

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            // Wood drawer blocks
            for (WoodDrawerBlock block : FunctionalStorageLgeacy.WOOD_DRAWER_BLOCKS) {
                registerBlockModel(block);
            }

            // Special blocks
            registerBlockModel(FunctionalStorageLgeacy.DRAWER_CONTROLLER_BLOCK);
            registerBlockModel(FunctionalStorageLgeacy.CONTROLLER_EXTENSION_BLOCK);
            registerBlockModel(FunctionalStorageLgeacy.COMPACTING_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLgeacy.SIMPLE_COMPACTING_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLgeacy.FLUID_DRAWER_1);
            registerBlockModel(FunctionalStorageLgeacy.FLUID_DRAWER_2);
            registerBlockModel(FunctionalStorageLgeacy.FLUID_DRAWER_4);
            registerBlockModel(FunctionalStorageLgeacy.ENDER_DRAWER_BLOCK);
            registerBlockModel(FunctionalStorageLgeacy.ARMORY_CABINET_BLOCK);

            // Items
            registerItemModel(FunctionalStorageLgeacy.IRON_DOWNGRADE);
            registerItemModel(FunctionalStorageLgeacy.COPPER_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.GOLD_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.DIAMOND_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.NETHERITE_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.CREATIVE_VENDING_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.VOID_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.REDSTONE_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.PULLING_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.PUSHING_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.COLLECTOR_UPGRADE);
            registerItemModel(FunctionalStorageLgeacy.CONFIGURATION_TOOL);
            registerItemModel(FunctionalStorageLgeacy.LINKING_TOOL);
        }

        private static void registerBlockModel(Block block) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(Objects.requireNonNull(block.getRegistryName()), "inventory"));
        }

        private static void registerItemModel(Item item) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
        }
    }
}
