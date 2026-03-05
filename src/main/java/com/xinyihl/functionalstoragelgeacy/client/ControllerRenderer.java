package com.xinyihl.functionalstoragelgeacy.client;

import com.xinyihl.functionalstoragelgeacy.block.tile.StorageControllerTile;
import com.xinyihl.functionalstoragelgeacy.item.LinkingToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * TileEntitySpecialRenderer for StorageControllerTile.
 * Renders:
 * 1. Controller search range box (green wireframe + translucent green faces)
 * 2. Connected drawers highlight (white wireframes)
 * 3. Linking tool area selection preview (white wireframe for two-point selection in MULTIPLE mode)
 * <p>
 * All rendering only occurs when the player holds a LinkingToolItem in their main hand
 * and the tool is bound to this controller.
 */
@SideOnly(Side.CLIENT)
public class ControllerRenderer extends TileEntitySpecialRenderer<StorageControllerTile> {

    @Override
    public void render(StorageControllerTile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null || te.getWorld() == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        ItemStack mainHand = mc.player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof LinkingToolItem)) return;

        // Only render if the tool is bound to THIS controller
        BlockPos controllerPos = LinkingToolItem.getControllerPos(mainHand);
        if (controllerPos == null || !controllerPos.equals(te.getPos())) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Disable texture, enable blending for translucent rendering
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        // ---- 1. Area selection preview (MULTIPLE mode, first position set) ----
        LinkingToolItem.LinkingMode mode = LinkingToolItem.getLinkingMode(mainHand);
        if (mode == LinkingToolItem.LinkingMode.MULTIPLE && hasFirstPosition(mainHand)) {
            BlockPos firstPos = getFirstPosition(mainHand);
            RayTraceResult rayTrace = mc.player.rayTrace(8, partialTicks);
            if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos hitPos = rayTrace.getBlockPos();
                AxisAlignedBB selectionBox = new AxisAlignedBB(
                        Math.min(firstPos.getX(), hitPos.getX()),
                        Math.min(firstPos.getY(), hitPos.getY()),
                        Math.min(firstPos.getZ(), hitPos.getZ()),
                        Math.max(firstPos.getX(), hitPos.getX()) + 1,
                        Math.max(firstPos.getY(), hitPos.getY()) + 1,
                        Math.max(firstPos.getZ(), hitPos.getZ()) + 1
                ).offset(-te.getPos().getX(), -te.getPos().getY(), -te.getPos().getZ());
                renderWireframeBox(selectionBox, 1f, 1f, 1f, 1f);
            }
        }

        // ---- 2. Connected drawers highlight (white wireframes) ----
        List<Long> connectedPositions = te.getConnectedDrawers().getConnectedDrawers();
        for (Long posLong : connectedPositions) {
            BlockPos drawerPos = BlockPos.fromLong(posLong);
            double dx = drawerPos.getX() - te.getPos().getX();
            double dy = drawerPos.getY() - te.getPos().getY();
            double dz = drawerPos.getZ() - te.getPos().getZ();
            AxisAlignedBB drawerBox = new AxisAlignedBB(dx, dy, dz, dx + 1, dy + 1, dz + 1);
            renderWireframeBox(drawerBox, 1f, 1f, 1f, 1f);
        }

        // ---- 3. Controller range box (green wireframe + translucent green faces) ----
        double range = te.getControllerRange();
        AxisAlignedBB rangeBox = new AxisAlignedBB(0, 0, 0, 1, 1, 1).grow(range).grow(0.002);
        renderWireframeBox(new AxisAlignedBB(0, 0, 0, 1, 1, 1).grow(0.002), 1f, 0.293f, 0.416f, 1f);
        GlStateManager.enableDepth();
        renderWireframeBox(rangeBox, 0.0f, 1f, 0.0f, 1f);
        renderGridBox(rangeBox, 0.0f, 1f, 0.0f, 0.5f);
        //renderFilledBox(rangeBox, 0.0f, 1f, 0.0f, 0.15f);

        // Restore GL state
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(StorageControllerTile te) {
        return true;
    }

    // ============================================================
    // Wireframe box rendering
    // ============================================================

    private void renderWireframeBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.glLineWidth(2.0f);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // Bottom face edges
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();

        // Top face edges
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();

        // Vertical edges
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();

        tessellator.draw();
    }

    private void renderGridBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.glLineWidth(1.1f);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // Draw grid lines on faces
        for (float x = (float) Math.ceil(x1); x < x2; x += 1.0f) {
            // Top and Bottom (YZ)
            buffer.pos(x, y1, z1).color(r, g, b, a).endVertex();
            buffer.pos(x, y1, z2).color(r, g, b, a).endVertex();
            buffer.pos(x, y2, z1).color(r, g, b, a).endVertex();
            buffer.pos(x, y2, z2).color(r, g, b, a).endVertex();
            // Front and Back (XY)
            buffer.pos(x, y1, z1).color(r, g, b, a).endVertex();
            buffer.pos(x, y2, z1).color(r, g, b, a).endVertex();
            buffer.pos(x, y1, z2).color(r, g, b, a).endVertex();
            buffer.pos(x, y2, z2).color(r, g, b, a).endVertex();
        }

        for (float y = (float) Math.ceil(y1); y < y2; y += 1.0f) {
            // Front and Back (XZ)
            buffer.pos(x1, y, z1).color(r, g, b, a).endVertex();
            buffer.pos(x2, y, z1).color(r, g, b, a).endVertex();
            buffer.pos(x1, y, z2).color(r, g, b, a).endVertex();
            buffer.pos(x2, y, z2).color(r, g, b, a).endVertex();
            // Left and Right (YZ)
            buffer.pos(x1, y, z1).color(r, g, b, a).endVertex();
            buffer.pos(x1, y, z2).color(r, g, b, a).endVertex();
            buffer.pos(x2, y, z1).color(r, g, b, a).endVertex();
            buffer.pos(x2, y, z2).color(r, g, b, a).endVertex();
        }

        for (float z = (float) Math.ceil(z1); z < z2; z += 1.0f) {
            // Top and Bottom (XZ)
            buffer.pos(x1, y1, z).color(r, g, b, a).endVertex();
            buffer.pos(x2, y1, z).color(r, g, b, a).endVertex();
            buffer.pos(x1, y2, z).color(r, g, b, a).endVertex();
            buffer.pos(x2, y2, z).color(r, g, b, a).endVertex();
            // Left and Right (XY)
            buffer.pos(x1, y1, z).color(r, g, b, a).endVertex();
            buffer.pos(x1, y2, z).color(r, g, b, a).endVertex();
            buffer.pos(x2, y1, z).color(r, g, b, a).endVertex();
            buffer.pos(x2, y2, z).color(r, g, b, a).endVertex();
        }

        tessellator.draw();
    }

    // ============================================================
    // Filled (translucent) box rendering
    // ============================================================

    private void renderFilledBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // Front face (z1) - both sides
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();

        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();

        // Back face (z2) - both sides
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();

        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();

        // Bottom face (y1) - both sides
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();

        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();

        // Top face (y2) - both sides
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();

        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();

        // Left face (x1) - both sides
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();

        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, z2).color(r, g, b, a).endVertex();

        // Right face (x2) - both sides
        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();

        buffer.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z1).color(r, g, b, a).endVertex();

        tessellator.draw();
    }

    // ============================================================
    // LinkingTool NBT helpers (accessing private state)
    // ============================================================

    private boolean hasFirstPosition(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("FirstPos");
    }

    private BlockPos getFirstPosition(ItemStack stack) {
        return BlockPos.fromLong(stack.getTagCompound().getLong("FirstPos"));
    }
}
