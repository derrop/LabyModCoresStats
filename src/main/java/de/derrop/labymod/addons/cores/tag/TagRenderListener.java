package de.derrop.labymod.addons.cores.tag;
/*
 * Created by derrop on 15.10.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import net.labymod.api.events.RenderEntityEvent;
import net.labymod.core.LabyModCore;
import net.labymod.core.WorldRendererAdapter;
import net.labymod.main.LabyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;

public class TagRenderListener implements RenderEntityEvent {
    private CoresAddon coresAddon;
    private TagProvider tagProvider;

    public TagRenderListener(CoresAddon coresAddon, TagProvider tagProvider) {
        this.coresAddon = coresAddon;
        this.tagProvider = tagProvider;
    }

    @Override
    public void onRender(Entity entity, double x, double y, double z, float partialTicks) {
        if (!this.coresAddon.getMainConfig().showTagsAboveName) {
            return;
        }
        if (!LabyMod.getSettings().showMyName && entity.getUniqueID().equals(LabyMod.getInstance().getPlayerUUID())) {
            return;
        }
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        OnlinePlayer player = this.coresAddon.getPlayerProvider().getOnlinePlayer(entity.getUniqueID());
        if (player == null) {
            return;
        }

        boolean canRender = Minecraft.isGuiEnabled() && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer) && entity.riddenByEntity == null;
        if (!canRender) {
            return;
        }

        double distance = entity.getDistanceSqToEntity(Minecraft.getMinecraft().getRenderManager().livingPlayer);
        float f = entity.isSneaking() ? 32.0F : 64.0F;
        if (distance < f * f) {
            Collection<Tag> tags = player.getCachedTags();

            int maxLineLength = 20;

            Collection<String> lines = new ArrayList<>();
            if (player.getLastStatistics() != null && player.getLastStatistics().hasRank()) {
                lines.add("Rang: " + player.getLastStatistics().getRank());
                lines.add("");
            }

            if (tags != null && !tags.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                tags.stream().map(Tag::getTag).forEach(tag -> {
                    if (builder.length() >= maxLineLength) {
                        lines.add(builder.substring(0, builder.length() - 2));
                        builder.setLength(0);
                    }

                    builder.append(tag).append(", ");
                });
                if (builder.length() > 0) {
                    lines.add(builder.substring(0, builder.length() - 2));
                }
            }

            if (lines.isEmpty()) {
                return;
            }

            for (String line : lines) {
                double size = 1D;
                if (!line.trim().isEmpty()) {
                    GlStateManager.pushMatrix();

                    y += 0.3;

                    GlStateManager.translate(0.0D, -0.2D + size / 8.0D, 0.0D);

                    renderLivingLabelCustom(entity, line, x, y, z, 64, (float)size);
                    GlStateManager.popMatrix();
                }

                y += size / 10.0D;
            }
        }

    }

    protected void renderLivingLabelCustom(Entity entityIn, String str, double x, double y, double z, int maxDistance, float scale) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        double d0 = entityIn.getDistanceSqToEntity(renderManager.livingPlayer);
        if (d0 <= maxDistance * maxDistance) {
            float fixedPlayerViewX = renderManager.playerViewX * (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -1 : 1);

            FontRenderer fontrenderer = renderManager.getFontRenderer();
            float f1 = 0.016666668F * scale;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(fixedPlayerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-f1, -f1, f1);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRendererAdapter worldrenderer = LabyModCore.getWorldRenderer();
            int i = 0;
            if (str.equals("deadmau5")) {
                i = -10;
            }
            int j = fontrenderer.getStringWidth(str) / 2;
            GlStateManager.disableTexture2D();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos(-j - 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(-j - 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(j + 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos(j + 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
            fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }
}
