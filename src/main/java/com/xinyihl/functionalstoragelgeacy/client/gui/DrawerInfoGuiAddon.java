package com.xinyihl.functionalstoragelgeacy.client.gui;

import com.xinyihl.functionalstoragelgeacy.util.NumberUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Renders drawer item info panel within the GUI.
 * Shows stored items with amounts in a 48x48 panel using background.png.
 * Ported from FunctionalStorage 1.21's DrawerInfoGuiAddon.
 */
public class DrawerInfoGuiAddon {

    private final int posX;
    private final int posY;
    private final ResourceLocation gui;
    private final int slotAmount;
    private final Function<Integer, Pair<Integer, Integer>> slotPosition;
    private final Function<Integer, ItemStack> slotStack;
    private final Function<Integer, Integer> slotMaxAmount;
    private final Function<Integer, ItemStack> slotLockedDisplay;

    public DrawerInfoGuiAddon(int posX, int posY, ResourceLocation gui, int slotAmount,
                              Function<Integer, Pair<Integer, Integer>> slotPosition,
                              Function<Integer, ItemStack> slotStack,
                              Function<Integer, Integer> slotMaxAmount,
                              Function<Integer, ItemStack> slotLockedDisplay) {
        this.posX = posX;
        this.posY = posY;
        this.gui = gui;
        this.slotAmount = slotAmount;
        this.slotPosition = slotPosition;
        this.slotStack = slotStack;
        this.slotMaxAmount = slotMaxAmount;
        this.slotLockedDisplay = slotLockedDisplay;
    }

    /**
     * Draw the background panel and items with amount text.
     */
    public void drawBackground(GuiScreen screen, int guiX, int guiY) {
        Minecraft mc = Minecraft.getMinecraft();
        int size = 48; // 16 * 2 + 16
        // Draw background.png as the base background
        // Draw front texture as the panel overlay
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(gui);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(guiX + posX, guiY + posY, 0, 0, size, size, size, size);
        GlStateManager.disableBlend();

        for (int i = 0; i < slotAmount; i++) {
            ItemStack itemStack = slotStack.apply(i);
            if (itemStack.isEmpty() && !slotLockedDisplay.apply(i).isEmpty()) {
                itemStack = slotLockedDisplay.apply(i);
            }
            if (!itemStack.isEmpty()) {
                int x = guiX + slotPosition.apply(i).getLeft() + posX;
                int y = guiY + slotPosition.apply(i).getRight() + posY;

                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, y);
                RenderHelper.disableStandardItemLighting();

                String amount = NumberUtils.getFormatedBigNumber(slotStack.apply(i).getCount())
                        + "/" + NumberUtils.getFormatedBigNumber(slotMaxAmount.apply(i));
                float scale = 0.5f;
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 200);
                GlStateManager.scale(scale, scale, scale);
                int textX = (int) ((x + 17 - mc.fontRenderer.getStringWidth(amount) / 2) * (1 / scale));
                int textY = (int) ((y + 12) * (1 / scale));
                mc.fontRenderer.drawStringWithShadow(amount, textX, textY, 0xFFFFFF);
                GlStateManager.popMatrix();
            }
        }
    }

    /**
     * Draw hover highlight and tooltip on mouse-over.
     */
    public void drawForeground(GuiScreen screen, int guiX, int guiY, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < slotAmount; i++) {
            int x = slotPosition.apply(i).getLeft() + posX + guiX;
            int y = slotPosition.apply(i).getRight() + posY + guiY;
            if (mouseX > x && mouseX < x + 18 && mouseY > y && mouseY < y + 18) {
                // Draw highlight (relative to gui origin for foreground layer)
                int fx = slotPosition.apply(i).getLeft() + posX;
                int fy = slotPosition.apply(i).getRight() + posY;
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.colorMask(true, true, true, false);
                Gui.drawRect(fx - 1, fy - 1, fx + 17, fy + 17, 0x80FFFFFF);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();

                // Build tooltip
                List<String> tooltip = new ArrayList<>();
                ItemStack over = slotStack.apply(i);
                if (over.isEmpty() && !slotLockedDisplay.apply(i).isEmpty()) {
                    over = slotLockedDisplay.apply(i);
                }
                if (over.isEmpty()) {
                    tooltip.add("\u00a76" + net.minecraft.client.resources.I18n.format("gui.functionalstoragelgeacy.item")
                            + "\u00a7f" + net.minecraft.client.resources.I18n.format("gui.functionalstoragelgeacy.empty"));
                } else {
                    tooltip.add("\u00a76" + net.minecraft.client.resources.I18n.format("gui.functionalstoragelgeacy.item")
                            + "\u00a7f" + over.getDisplayName());
                    String amountStr = NumberUtils.getFormattedNumber(slotStack.apply(i).getCount())
                            + "/" + NumberUtils.getFormattedNumber(slotMaxAmount.apply(i));
                    tooltip.add("\u00a76" + net.minecraft.client.resources.I18n.format("gui.functionalstoragelgeacy.amount")
                            + "\u00a7f" + amountStr);
                }
                tooltip.add("\u00a76" + net.minecraft.client.resources.I18n.format("gui.functionalstoragelgeacy.slot")
                        + "\u00a7f" + i);

                screen.drawHoveringText(tooltip, mouseX - guiX, mouseY - guiY);
            }
        }
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}
