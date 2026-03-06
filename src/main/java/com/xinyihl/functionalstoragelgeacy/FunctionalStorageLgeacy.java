package com.xinyihl.functionalstoragelgeacy;

import com.xinyihl.functionalstoragelgeacy.block.*;
import com.xinyihl.functionalstoragelgeacy.block.tile.ControllableDrawerTile;
import com.xinyihl.functionalstoragelgeacy.compat.top.TheOneProbeCompat;
import com.xinyihl.functionalstoragelgeacy.config.FunctionalStorageConfig;
import com.xinyihl.functionalstoragelgeacy.item.*;
import com.xinyihl.functionalstoragelgeacy.network.NetworkHandler;
import com.xinyihl.functionalstoragelgeacy.proxy.CommonProxy;
import com.xinyihl.functionalstoragelgeacy.util.DrawerWoodType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class FunctionalStorageLgeacy {

    @Mod.Instance(Tags.MOD_ID)
    public static FunctionalStorageLgeacy INSTANCE;

    // ====== Blocks ======
    // Wood drawers
    public static final List<WoodDrawerBlock> WOOD_DRAWER_BLOCKS = new ArrayList<>();

    // ====== Creative Tab ======
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(Tags.MOD_ID) {
        @Nonnull
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(DRAWER_CONTROLLER_BLOCK);
        }
    };
    @SidedProxy(clientSide = "com.xinyihl.functionalstoragelgeacy.proxy.ClientProxy", serverSide = "com.xinyihl.functionalstoragelgeacy.proxy.CommonProxy")
    public static CommonProxy proxy;
    public static DrawerControllerBlock DRAWER_CONTROLLER_BLOCK;
    public static ControllerExtensionBlock CONTROLLER_EXTENSION_BLOCK;
    public static CompactingDrawerBlock COMPACTING_DRAWER_BLOCK;
    public static SimpleCompactingDrawerBlock SIMPLE_COMPACTING_DRAWER_BLOCK;
    public static FluidDrawerBlock FLUID_DRAWER_1;
    public static FluidDrawerBlock FLUID_DRAWER_2;
    public static FluidDrawerBlock FLUID_DRAWER_4;
    public static EnderDrawerBlock ENDER_DRAWER_BLOCK;
    public static ArmoryCabinetBlock ARMORY_CABINET_BLOCK;

    // ====== Items ======
    // Storage Upgrades
    public static StorageUpgradeItem IRON_DOWNGRADE;
    public static StorageUpgradeItem COPPER_UPGRADE;
    public static StorageUpgradeItem GOLD_UPGRADE;
    public static StorageUpgradeItem DIAMOND_UPGRADE;
    public static StorageUpgradeItem NETHERITE_UPGRADE;
    public static Item CREATIVE_VENDING_UPGRADE;

    // Utility Upgrades
    public static UpgradeItem VOID_UPGRADE;
    public static UpgradeItem REDSTONE_UPGRADE;
    public static UpgradeItem PULLING_UPGRADE;
    public static UpgradeItem PUSHING_UPGRADE;
    public static UpgradeItem COLLECTOR_UPGRADE;

    // Tools
    public static ConfigurationToolItem CONFIGURATION_TOOL;
    public static LinkingToolItem LINKING_TOOL;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FunctionalStorageConfig.init(event.getSuggestedConfigurationFile());
        TheOneProbeCompat.register();
        NetworkHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    // ====== Registration ======
    @Mod.EventBusSubscriber(modid = Tags.MOD_ID)
    public static class RegistrationHandler {

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            // Wood drawers
            for (DrawerWoodType wood : DrawerWoodType.values()) {
                for (DrawerType type : DrawerType.values()) {
                    WoodDrawerBlock block = new WoodDrawerBlock(wood, type);
                    WOOD_DRAWER_BLOCKS.add(block);
                    event.getRegistry().register(block);
                }
            }

            // Special blocks
            DRAWER_CONTROLLER_BLOCK = new DrawerControllerBlock();
            CONTROLLER_EXTENSION_BLOCK = new ControllerExtensionBlock();
            COMPACTING_DRAWER_BLOCK = new CompactingDrawerBlock();
            SIMPLE_COMPACTING_DRAWER_BLOCK = new SimpleCompactingDrawerBlock();
            FLUID_DRAWER_1 = new FluidDrawerBlock(DrawerType.X_1);
            FLUID_DRAWER_2 = new FluidDrawerBlock(DrawerType.X_2);
            FLUID_DRAWER_4 = new FluidDrawerBlock(DrawerType.X_4);
            ENDER_DRAWER_BLOCK = new EnderDrawerBlock();
            ARMORY_CABINET_BLOCK = new ArmoryCabinetBlock();

            event.getRegistry().registerAll(
                    DRAWER_CONTROLLER_BLOCK,
                    CONTROLLER_EXTENSION_BLOCK,
                    COMPACTING_DRAWER_BLOCK,
                    SIMPLE_COMPACTING_DRAWER_BLOCK,
                    FLUID_DRAWER_1,
                    FLUID_DRAWER_2,
                    FLUID_DRAWER_4,
                    ENDER_DRAWER_BLOCK,
                    ARMORY_CABINET_BLOCK
            );
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            // ItemBlocks for all blocks
            for (WoodDrawerBlock block : WOOD_DRAWER_BLOCKS) {
                event.getRegistry().register(createItemBlock(block));
            }
            event.getRegistry().registerAll(
                    createItemBlock(DRAWER_CONTROLLER_BLOCK),
                    createItemBlock(CONTROLLER_EXTENSION_BLOCK),
                    createItemBlock(COMPACTING_DRAWER_BLOCK),
                    createItemBlock(SIMPLE_COMPACTING_DRAWER_BLOCK),
                    createItemBlock(FLUID_DRAWER_1),
                    createItemBlock(FLUID_DRAWER_2),
                    createItemBlock(FLUID_DRAWER_4),
                    createItemBlock(ENDER_DRAWER_BLOCK),
                    createItemBlock(ARMORY_CABINET_BLOCK)
            );

            // Storage Upgrades
            IRON_DOWNGRADE = new StorageUpgradeItem(StorageUpgradeItem.StorageTier.IRON);
            IRON_DOWNGRADE.setRegistryName("iron_downgrade");
            IRON_DOWNGRADE.setTranslationKey(Tags.MOD_ID + ".iron_downgrade");

            COPPER_UPGRADE = new StorageUpgradeItem(StorageUpgradeItem.StorageTier.COPPER);
            COPPER_UPGRADE.setRegistryName("copper_upgrade");
            COPPER_UPGRADE.setTranslationKey(Tags.MOD_ID + ".copper_upgrade");

            GOLD_UPGRADE = new StorageUpgradeItem(StorageUpgradeItem.StorageTier.GOLD);
            GOLD_UPGRADE.setRegistryName("gold_upgrade");
            GOLD_UPGRADE.setTranslationKey(Tags.MOD_ID + ".gold_upgrade");

            DIAMOND_UPGRADE = new StorageUpgradeItem(StorageUpgradeItem.StorageTier.DIAMOND);
            DIAMOND_UPGRADE.setRegistryName("diamond_upgrade");
            DIAMOND_UPGRADE.setTranslationKey(Tags.MOD_ID + ".diamond_upgrade");

            NETHERITE_UPGRADE = new StorageUpgradeItem(StorageUpgradeItem.StorageTier.NETHERITE);
            NETHERITE_UPGRADE.setRegistryName("netherite_upgrade");
            NETHERITE_UPGRADE.setTranslationKey(Tags.MOD_ID + ".netherite_upgrade");

            CREATIVE_VENDING_UPGRADE = new Item()
                    .setRegistryName("creative_vending_upgrade")
                    .setTranslationKey(Tags.MOD_ID + ".creative_vending_upgrade")
                    .setMaxStackSize(1)
                    .setCreativeTab(CREATIVE_TAB);

            // Utility Upgrades
            VOID_UPGRADE = new UpgradeItem(UpgradeItem.Type.UTILITY, UpgradeItem.UtilityAction.VOID);
            VOID_UPGRADE.setRegistryName("void_upgrade");
            VOID_UPGRADE.setTranslationKey(Tags.MOD_ID + ".void_upgrade");

            REDSTONE_UPGRADE = new UpgradeItem(UpgradeItem.Type.UTILITY, UpgradeItem.UtilityAction.REDSTONE);
            REDSTONE_UPGRADE.setRegistryName("redstone_upgrade");
            REDSTONE_UPGRADE.setTranslationKey(Tags.MOD_ID + ".redstone_upgrade");

            PULLING_UPGRADE = new UpgradeItem(UpgradeItem.Type.UTILITY, UpgradeItem.UtilityAction.PULLING);
            PULLING_UPGRADE.setRegistryName("pulling_upgrade");
            PULLING_UPGRADE.setTranslationKey(Tags.MOD_ID + ".pulling_upgrade");

            PUSHING_UPGRADE = new UpgradeItem(UpgradeItem.Type.UTILITY, UpgradeItem.UtilityAction.PUSHING);
            PUSHING_UPGRADE.setRegistryName("pushing_upgrade");
            PUSHING_UPGRADE.setTranslationKey(Tags.MOD_ID + ".pushing_upgrade");

            COLLECTOR_UPGRADE = new UpgradeItem(UpgradeItem.Type.UTILITY, UpgradeItem.UtilityAction.COLLECTOR);
            COLLECTOR_UPGRADE.setRegistryName("collector_upgrade");
            COLLECTOR_UPGRADE.setTranslationKey(Tags.MOD_ID + ".collector_upgrade");

            // Tools
            CONFIGURATION_TOOL = new ConfigurationToolItem();
            CONFIGURATION_TOOL.setRegistryName("configuration_tool");
            CONFIGURATION_TOOL.setTranslationKey(Tags.MOD_ID + ".configuration_tool");

            LINKING_TOOL = new LinkingToolItem();
            LINKING_TOOL.setRegistryName("linking_tool");
            LINKING_TOOL.setTranslationKey(Tags.MOD_ID + ".linking_tool");

            event.getRegistry().registerAll(
                    IRON_DOWNGRADE,
                    COPPER_UPGRADE,
                    GOLD_UPGRADE,
                    DIAMOND_UPGRADE,
                    NETHERITE_UPGRADE,
                    CREATIVE_VENDING_UPGRADE,
                    VOID_UPGRADE,
                    REDSTONE_UPGRADE,
                    PULLING_UPGRADE,
                    PUSHING_UPGRADE,
                    COLLECTOR_UPGRADE,
                    CONFIGURATION_TOOL,
                    LINKING_TOOL
            );
        }

        private static ItemBlock createItemBlock(Block block) {
            ItemBlock itemBlock = block instanceof DrawerBlock ? new DrawerItemBlock(block) : new ItemBlock(block);
            itemBlock.setRegistryName(block.getRegistryName());
            return itemBlock;
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            EntityPlayer player = event.getEntityPlayer();
            if (!player.isCreative()) return;

            World world = event.getWorld();
            BlockPos pos = event.getPos();
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (!(block instanceof DrawerBlock)) return;

            DrawerBlock drawerBlock = (DrawerBlock) block;
            int slot = drawerBlock.getHitSlot(state, world, pos, player);

            if (slot != -1) {
                event.setCanceled(true);
                if (!world.isRemote) {
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof ControllableDrawerTile) {
                        ((ControllableDrawerTile) te).onClicked(player, slot);
                    }
                }
            }
        }
    }
}
