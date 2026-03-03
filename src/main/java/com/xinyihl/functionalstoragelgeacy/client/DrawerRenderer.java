package com.xinyihl.functionalstoragelgeacy.client;

import com.xinyihl.functionalstoragelgeacy.DrawerType;
import com.xinyihl.functionalstoragelgeacy.block.DrawerBlock;
import com.xinyihl.functionalstoragelgeacy.block.tile.*;
import com.xinyihl.functionalstoragelgeacy.fluid.BigFluidHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.BigInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelgeacy.util.NumberUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

/**
 * TileEntity Special Renderer for drawing item/fluid icons and counts on drawer faces.
 * Handles all drawer types: standard (X_1/X_2/X_4), compacting, simple compacting, ender, and fluid.
 */
public class DrawerRenderer extends TileEntitySpecialRenderer<ControllableDrawerTile> {

    @Override
    public void render(ControllableDrawerTile te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        if (te == null || te.getWorld() == null) return;

        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof DrawerBlock)) return;

        DrawerBlock block = (DrawerBlock) state.getBlock();
        EnumFacing facing = state.getValue(DrawerBlock.FACING);
        ControllableDrawerTile.DrawerOptions options = te.getDrawerOptions();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        int ambLight = te.getWorld().getCombinedLight(te.getPos().offset(facing), 0);
        int lu = ambLight % 65536;
        int lv = ambLight / 65536;
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float)lu / 1.0F, (float)lv / 1.0F);

        setupFaceTransform(facing);

        if (te instanceof FluidDrawerTile) {
            renderFluidSlots((FluidDrawerTile) te, block.getDrawerType(), options);
        } else if (te instanceof SimpleCompactingDrawerTile) {
            renderSimpleCompactingSlots((SimpleCompactingDrawerTile) te, options);
        } else if (te instanceof CompactingDrawerTile) {
            renderCompactingSlots((CompactingDrawerTile) te, options);
        } else if (te instanceof EnderDrawerTile) {
            renderEnderSlot((EnderDrawerTile) te, options);
        } else {
            renderItemSlots(te, block.getDrawerType(), options);
        }

        renderLockOnFace(te);

        GlStateManager.enableLighting();
        GlStateManager.enableLight(0);
        GlStateManager.enableLight(1);
        GlStateManager.enableColorMaterial();
        GlStateManager.colorMaterial(1032, 5634);
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableNormalize();
        GlStateManager.disableBlend();

        GlStateManager.popMatrix();
    }

    private static final net.minecraft.util.ResourceLocation LOCK_TEXTURE = new net.minecraft.util.ResourceLocation("functionalstoragelgeacy", "textures/blocks/lock.png");

    private void renderLockOnFace(ControllableDrawerTile te) {
        if (!te.isLocked()) return;

        GlStateManager.pushMatrix();

        float offsetX = 0.5f;
        float offsetY = 15.5f / 16.0f;
        float zOffset = 1.01f;

        GlStateManager.translate(offsetX, offsetY, zOffset);
        float size = 0.5f / 16.0f;
        GlStateManager.scale(size, size, 1.0f);

        Minecraft.getMinecraft().getTextureManager().bindTexture(LOCK_TEXTURE);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);

        buffer.pos(-1, -1, 0).tex(0, 1).endVertex();
        buffer.pos(1, -1, 0).tex(1, 1).endVertex();
        buffer.pos(1, 1, 0).tex(1, 0).endVertex();
        buffer.pos(-1, 1, 0).tex(0, 0).endVertex();

        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();

        GlStateManager.popMatrix();
    }

    // ============================================================
    // Face transformation
    // ============================================================

    /**
     * Set up the GL matrix so the coordinate system is face-local:
     * origin at bottom-left of the front face (viewed from outside),
     * X+ goes right, Y+ goes up, Z+ goes outward from the block.
     */
    private void setupFaceTransform(EnumFacing facing) {
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        float rotY = 0;
        switch (facing) {
            case NORTH: rotY = 180; break;
            case SOUTH: rotY = 0; break;
            case WEST: rotY = 270; break;
            case EAST: rotY = 90; break;
            default: break;
        }
        GlStateManager.rotate(rotY, 0, 1, 0);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
    }

    // ============================================================
    // Standard item drawers (X_1 / X_2 / X_4)
    // ============================================================

    private void renderItemSlots(ControllableDrawerTile te, DrawerType drawerType,
                                 ControllableDrawerTile.DrawerOptions options) {
        if (drawerType == null) return;
        IItemHandler handler = te.getItemHandler();
        if (!(handler instanceof BigInventoryHandler)) return;
        BigInventoryHandler bigHandler = (BigInventoryHandler) handler;

        boolean showRender = options == null || options.isShowItemRender();
        boolean showCount = options == null || options.isShowItemCount();

        switch (drawerType) {
            case X_1:
                renderBigSlot(bigHandler, 0, 0.5F, 0.5F, 1.0F, showRender, showCount, 0.3F);
                break;
            case X_2:
                renderBigSlot(bigHandler, 0, 0.5F, 0.77F, 0.5F, showRender, showCount, 0.2F);
                renderBigSlot(bigHandler, 1, 0.5F, 0.27F, 0.5F, showRender, showCount, 0.2F);
                break;
            case X_4:
                renderBigSlot(bigHandler, 0, 0.75F, 0.77F, 0.5F, showRender, showCount, 0.2F); // Top-Right
                renderBigSlot(bigHandler, 1, 0.25F, 0.77F, 0.5F, showRender, showCount, 0.2F); // Top-Left
                renderBigSlot(bigHandler, 2, 0.75F, 0.27F, 0.5F, showRender, showCount, 0.2F); // Bottom-Right
                renderBigSlot(bigHandler, 3, 0.25F, 0.27F, 0.5F, showRender, showCount, 0.2F); // Bottom-Left
                break;
        }
    }

    private void renderBigSlot(BigInventoryHandler handler, int slot, float posX, float posY,
                               float slotScale, boolean showRender, boolean showCount, float textScale) {
        if (slot >= handler.getStoredStacks().size()) return;
        BigInventoryHandler.BigStack bigStack = handler.getBigStack(slot);
        ItemStack stack = bigStack.getStack();
        if (stack.isEmpty()) return;

        long count = bigStack.getAmount();
        if (showRender) {
            renderStackOnFace(stack, posX, posY, slotScale);
        }
        if (showCount) {
            renderCountOnFace(count, posX, posY, slotScale, textScale);
        }
    }

    // ============================================================
    // Compacting drawers (3 slots)
    // ============================================================

    private void renderCompactingSlots(CompactingDrawerTile te, ControllableDrawerTile.DrawerOptions options) {
        CompactingInventoryHandler handler = te.getHandler();
        if (handler == null) return;

        boolean showRender = options == null || options.isShowItemRender();
        boolean showCount = options == null || options.isShowItemCount();

        // 3 slots: 0 = bottom-right, 1 = bottom-left, 2 = top-center
        renderCompactSlot(handler, 0, 0.75F, 0.27F, showRender, showCount);
        renderCompactSlot(handler, 1, 0.25F, 0.27F, showRender, showCount);
        renderCompactSlot(handler, 2, 0.5F,  0.77F, showRender, showCount);
    }

    // ============================================================
    // Simple compacting drawers (2 slots)
    // ============================================================

    private void renderSimpleCompactingSlots(SimpleCompactingDrawerTile te, ControllableDrawerTile.DrawerOptions options) {
        CompactingInventoryHandler handler = te.getHandler();
        if (handler == null) return;

        boolean showRender = options == null || options.isShowItemRender();
        boolean showCount = options == null || options.isShowItemCount();

        renderCompactSlot(handler, 0, 0.5F, 0.27F, showRender, showCount);
        renderCompactSlot(handler, 1, 0.5F, 0.77F, showRender, showCount);
    }

    private void renderCompactSlot(CompactingInventoryHandler handler, int slot,
                                   float posX, float posY, boolean showRender, boolean showCount) {
        if (slot >= handler.getResults().size()) return;
        CompactingInventoryHandler.Result result = handler.getResults().get(slot);
        ItemStack stack = result.getStack();
        if (stack.isEmpty()) return;

        int count = handler.getStackInSlot(slot).getCount();
        if (showRender) {
            renderStackOnFace(stack, posX, posY, 0.5F);
        }
        if (showCount) {
            renderCountOnFace(count, posX, posY, 0.5F, 0.2F);
        }
    }

    // ============================================================
    // Ender drawers (1 slot)
    // ============================================================

    private void renderEnderSlot(EnderDrawerTile te, ControllableDrawerTile.DrawerOptions options) {
        IItemHandler handler = te.getItemHandler();
        if (handler == null || handler.getSlots() < 1) return;

        boolean showRender = options == null || options.isShowItemRender();
        boolean showCount = options == null || options.isShowItemCount();

        ItemStack stack = handler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        int count = stack.getCount();
        if (showRender) {
            renderStackOnFace(stack, 0.5F, 0.5F, 1.0F);
        }
        if (showCount) {
            renderCountOnFace(count, 0.5F, 0.5F, 1.0F, 0.3F);
        }
    }

    // ============================================================
    // Fluid drawers
    // ============================================================

    private void renderFluidSlots(FluidDrawerTile te, DrawerType drawerType,
                                  ControllableDrawerTile.DrawerOptions options) {
        BigFluidHandler handler = te.getFluidHandler();
        if (handler == null || drawerType == null) return;

        boolean showRender = options == null || options.isShowItemRender();
        boolean showCount = options == null || options.isShowItemCount();

        switch (drawerType) {
            case X_1:
                renderSingleFluidSlot(handler, 0, 0.5F, 0.5F, 1.0F, showRender, showCount, 0.3F);
                break;
            case X_2:
                renderSingleFluidSlot(handler, 0, 0.5F, 0.77F, 0.5F, showRender, showCount, 0.2F);
                renderSingleFluidSlot(handler, 1, 0.5F, 0.27F, 0.5F, showRender, showCount, 0.2F);
                break;
            case X_4:
                renderSingleFluidSlot(handler, 0, 0.75F, 0.77F, 0.5F, showRender, showCount, 0.2F); // Top-Right
                renderSingleFluidSlot(handler, 1, 0.25F, 0.77F, 0.5F, showRender, showCount, 0.2F); // Top-Left
                renderSingleFluidSlot(handler, 2, 0.75F, 0.27F, 0.5F, showRender, showCount, 0.2F); // Bottom-Right
                renderSingleFluidSlot(handler, 3, 0.25F, 0.27F, 0.5F, showRender, showCount, 0.2F); // Bottom-Left
                break;
        }
    }

    private void renderSingleFluidSlot(BigFluidHandler handler, int slot, float posX, float posY,
                                       float slotScale, boolean showRender, boolean showCount, float textScale) {
        if (slot >= handler.getTanksCount()) return;
        FluidStack fluid = handler.getTankFluid(slot);
        if (fluid == null || fluid.amount <= 0) return;

        if (showRender) {
            renderFluidOnFace(fluid, posX, posY, slotScale);
        }
        if (showCount) {
            renderCountOnFace(fluid.amount, posX, posY, slotScale, textScale);
        }
    }

    // ============================================================
    // Core rendering: item stack on face
    // ============================================================

    /**
     * Render an ItemStack icon on the drawer face.
     * Coordinates are in face-local space: (0,0) bottom-left, (1,1) top-right.
     *
     * @param stack     the item to render
     * @param posX      horizontal center position on face (0–1)
     * @param posY      vertical center position on face (0–1)
     * @param slotScale sub-scale for multi-slot drawers (1.0 for X_1, 0.5 for X_2/X_4)
     */
    private void renderStackOnFace(ItemStack stack, float posX, float posY, float slotScale) {
        if (stack.isEmpty()) return;

        float cX = posX * 16.0f;
        float cY = posY * 16.0f;
        float size = (slotScale >= 1.0f) ? 0.5f : 0.25f;

        float offsetX = cX - 8.0f * size;
        float offsetY = 16.0f - cY - 8.0f * size;

        GlStateManager.pushMatrix();

        GlStateManager.translate(0, 1, 1.005f);
        GlStateManager.scale(1 / 16f, -1 / 16f, 0.00001f);
        GlStateManager.translate(offsetX, offsetY, 0);
        GlStateManager.scale(size, size, 1);

        GlStateManager.pushMatrix();
        if (size >= 0.5f) {
            GlStateManager.scale(2.6f, 2.6f, 1);
            GlStateManager.rotate(171.6f, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(84.9f, 1.0F, 0.0F, 0.0F);
        } else {
            GlStateManager.scale(1.92f, 1.92f, 1);
            GlStateManager.rotate(169.2f, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(79.0f, 1.0F, 0.0F, 0.0F);
        }
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();

        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1, -1);

        GlStateManager.enableRescaleNormal();
        GlStateManager.disableRescaleNormal();
        GlStateManager.pushAttrib();
        GlStateManager.enableRescaleNormal();
        GlStateManager.popAttrib();

        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        try {
            renderItem.renderItemIntoGUI(stack, 0, 0);
        } catch (Exception ignored) {}

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disablePolygonOffset();

        GlStateManager.popMatrix();
    }

    // ============================================================
    // Core rendering: fluid icon on face
    // ============================================================

    private void renderFluidOnFace(FluidStack fluid, float posX, float posY, float slotScale) {
        if (fluid == null || fluid.getFluid() == null) return;

        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        if (sprite == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 1.001F);

        if (slotScale != 1.0F) {
            GlStateManager.scale(slotScale, slotScale, 1.0F);
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int color = fluid.getFluid().getColor(fluid);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;

        GlStateManager.color(r, g, b, a);
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        float half = 0.35F;
        buffer.pos(-half, -half, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        buffer.pos(half, -half, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        buffer.pos(half, half, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        buffer.pos(-half, half, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    // ============================================================
    // Core rendering: count text on face
    // ============================================================

    /**
     * Render the item/fluid count below the icon on the drawer face.
     *
     * @param count     the amount to display
     * @param posX      horizontal center of the slot
     * @param posY      vertical center of the slot
     * @param slotScale sub-scale for multi-slot drawers
     * @param maxScale  maximum text scale
     */
    private void renderCountOnFace(long count, float posX, float posY, float slotScale, float maxScale) {
        if (count <= 0) return;
        String text = NumberUtils.getFormattedBigNumber(count);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        float cX = posX * 16.0f;
        float cY = posY * 16.0f;
        float size = (slotScale >= 1.0f) ? 0.5f : 0.25f;

        float offsetX = cX;
        float offsetY = 16.0f - cY + 4.0f * size; // shift down

        GlStateManager.pushMatrix();

        float zOffset = 1.006f; // slightly in front of items
        GlStateManager.translate(0, 1, zOffset);
        GlStateManager.scale(1 / 16f, -1 / 16f, 0.00001f);
        GlStateManager.translate(offsetX, offsetY, 0);

        int textWidth = fontRenderer.getStringWidth(text);
        float scaleX = (16.0f * size) / (float) Math.max(textWidth, 1);
        float actualScale = Math.min(scaleX * 0.8f, maxScale);

        GlStateManager.scale(actualScale, actualScale, 1.0f);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        fontRenderer.drawString(text, -textWidth / 2, 4, 0xFFFFFFFF, true);

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
