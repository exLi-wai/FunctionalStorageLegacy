package com.xinyihl.functionalstoragelgeacy.client.gui;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.Tags;
import com.xinyihl.functionalstoragelgeacy.block.tile.*;
import com.xinyihl.functionalstoragelgeacy.container.ContainerDrawer;
import com.xinyihl.functionalstoragelgeacy.fluid.BigFluidHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.BigInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.CompactingInventoryHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

public class GuiDrawer extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/background.png");

    private static final ResourceLocation SLOT = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/slot.png");

    private static final int INFO_PANEL_X = 120;
    private static final int INFO_PANEL_Y = 8;

    private final ContainerDrawer container;
    private DrawerInfoGuiAddon itemInfoAddon;
    private FluidDrawerInfoGuiAddon fluidInfoAddon;

    public GuiDrawer(ContainerDrawer container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 166;
        initInfoAddon();
    }

    private void initInfoAddon() {
        ControllableDrawerTile tile = container.getTile();

        if (tile instanceof DrawerTile) {
            DrawerTile drawerTile = (DrawerTile) tile;
            DrawerType type = drawerTile.getDrawerType();
            BigInventoryHandler handler = drawerTile.getHandler();
            ResourceLocation frontTexture = new ResourceLocation(Tags.MOD_ID,
                    "textures/blocks/" + drawerTile.getWoodType().getName() + "_front_" + type.getSlots() + ".png");
            itemInfoAddon = new DrawerInfoGuiAddon(
                    INFO_PANEL_X, INFO_PANEL_Y,
                    frontTexture,
                    type.getSlots(),
                    type.getSlotPosition(),
                    i -> {
                        BigInventoryHandler.BigStack bs = handler.getBigStack(i);
                        if (bs.getAmount() > 0) {
                            ItemStack display = bs.getStack().copy();
                            display.setCount(bs.getAmount());
                            return display;
                        }
                        return ItemStack.EMPTY;
                    },
                    i -> handler.getSlotLimit(i),
                    i -> {
                        if (drawerTile.isLocked()) {
                            BigInventoryHandler.BigStack bs = handler.getBigStack(i);
                            if (!bs.getStack().isEmpty()) {
                                ItemStack locked = bs.getStack().copy();
                                locked.setCount(1);
                                return locked;
                            }
                        }
                        return ItemStack.EMPTY;
                    }
            );
        } else if (tile instanceof CompactingDrawerTile) {
            CompactingDrawerTile compactingTile = (CompactingDrawerTile) tile;
            CompactingInventoryHandler handler = compactingTile.getCompactingHandler();
            int slots = handler.getSlots();
            if (handler.isVoid()) slots--; // exclude void slot
            final int slotCount = slots;
            Function<Integer, Pair<Integer, Integer>> positions = getCompactingPositions(slotCount);
            ResourceLocation frontTexture;
            if (tile instanceof SimpleCompactingDrawerTile) {
                frontTexture = new ResourceLocation(Tags.MOD_ID, "textures/blocks/simple_compacting_drawer_front.png");
            } else {
                frontTexture = new ResourceLocation(Tags.MOD_ID, "textures/blocks/compacting_drawer_front.png");
            }
            itemInfoAddon = new DrawerInfoGuiAddon(
                    INFO_PANEL_X, INFO_PANEL_Y,
                    frontTexture,
                    slotCount,
                    positions,
                    i -> handler.getStackInSlot(i),
                    i -> handler.getSlotLimit(i),
                    i -> {
                        if (compactingTile.isLocked() && handler.isSetup()) {
                            java.util.List<CompactingInventoryHandler.Result> results = handler.getResults();
                            if (i < results.size() && !results.get(i).getStack().isEmpty()) {
                                ItemStack locked = results.get(i).getStack().copy();
                                locked.setCount(1);
                                return locked;
                            }
                        }
                        return ItemStack.EMPTY;
                    }
            );
        } else if (tile instanceof FluidDrawerTile) {
            FluidDrawerTile fluidTile = (FluidDrawerTile) tile;
            DrawerType type = fluidTile.getDrawerType();
            BigFluidHandler handler = fluidTile.getFluidHandler();
            String suffix = type.getSlots() == 1 ? "" : "_" + type.getSlots();
            ResourceLocation frontTexture = new ResourceLocation(Tags.MOD_ID,
                    "textures/blocks/fluid_front" + suffix + ".png");
            fluidInfoAddon = new FluidDrawerInfoGuiAddon(
                    INFO_PANEL_X, INFO_PANEL_Y,
                    frontTexture,
                    type.getSlots(),
                    type.getSlotPosition(),
                    () -> handler,
                    i -> handler.getCapacityPerTank()
            );
        } else if (tile instanceof EnderDrawerTile) {
            EnderDrawerTile enderTile = (EnderDrawerTile) tile;
            ResourceLocation frontTexture = new ResourceLocation(Tags.MOD_ID, "textures/blocks/ender_front_1.png");
            itemInfoAddon = new DrawerInfoGuiAddon(
                    INFO_PANEL_X, INFO_PANEL_Y,
                    frontTexture,
                    1,
                    DrawerType.X_1.getSlotPosition(),
                    i -> {
                        if (enderTile.getItemHandler() != null) {
                            return enderTile.getItemHandler().getStackInSlot(0);
                        }
                        return ItemStack.EMPTY;
                    },
                    i -> {
                        if (enderTile.getItemHandler() != null) {
                            return enderTile.getItemHandler().getSlotLimit(0);
                        }
                        return 0;
                    },
                    i -> {
                        if (enderTile.isLocked() && enderTile.getItemHandler() != null) {
                            ItemStack stack = enderTile.getItemHandler().getStackInSlot(0);
                            if (!stack.isEmpty()) {
                                ItemStack locked = stack.copy();
                                locked.setCount(1);
                                return locked;
                            }
                        }
                        return ItemStack.EMPTY;
                    }
            );
        }
    }

    private Function<Integer, Pair<Integer, Integer>> getCompactingPositions(int slotCount) {
        if (slotCount == 2) {
            return DrawerType.X_2.getSlotPosition();
        }
        // 3 slots: matches block face layout (top-center + bottom-left + bottom-right)
        return i -> {
            if (i == 0) return Pair.of(28, 28);  // bottom-right (highest tier)
            if (i == 1) return Pair.of(4, 28);   // bottom-left (mid tier)
            return Pair.of(16, 4);                // top-center (lowest tier)
        };
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        // Storage upgrade slot backgrounds
        int storageSlots = container.getTile().getStorageUpgrades().getSlots();
        for (int i = 0; i < storageSlots; i++) {
            int sx = x + 7 + i * 18;
            int sy = y + 19;
            this.drawTexturedModalRect(sx, sy, 238, 0, 18, 18);
        }

        // Utility upgrade slot backgrounds
        int utilitySlots = container.getTile().getUtilityUpgrades().getSlots();
        for (int i = 0; i < utilitySlots; i++) {
            int sx = x + 7 + i * 18;
            int sy = y + 43;
            this.drawTexturedModalRect(sx, sy, 238, 0, 18, 18);
        }

        // Draw info panel
        if (itemInfoAddon != null) {
            itemInfoAddon.drawBackground(this, x, y);
        }
        if (fluidInfoAddon != null) {
            fluidInfoAddon.drawBackground(this, x, y);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = container.getTile().getDisplayName() != null
                ? container.getTile().getDisplayName().getUnformattedText()
                : "Drawer";
        this.fontRenderer.drawString(title, 8, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);

        // Draw info panel tooltips
        int guiX = (this.width - this.xSize) / 2;
        int guiY = (this.height - this.ySize) / 2;
        if (itemInfoAddon != null) {
            itemInfoAddon.drawForeground(this, guiX, guiY, mouseX, mouseY);
        }
        if (fluidInfoAddon != null) {
            fluidInfoAddon.drawForeground(this, guiX, guiY, mouseX, mouseY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
