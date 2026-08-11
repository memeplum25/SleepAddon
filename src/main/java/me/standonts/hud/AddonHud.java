package me.standonts.hud;

import fr.alexdoru.configlib.api.IRenderer;
import fr.alexdoru.configlib.api.RendererPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

abstract class AddonHud extends Gui implements IRenderer {

    protected static final Minecraft MC = Minecraft.getMinecraft();
    private final RendererPosition position;

    AddonHud(RendererPosition position) {
        this.position = position;
    }

    @Override
    public final RendererPosition getPosition() {
        return position;
    }

    protected final void drawPlayerHead(EntityPlayer player, int x, int y) {
        ResourceLocation skin = DefaultPlayerSkin.getDefaultSkinLegacy();
        if (player != null && MC.getNetHandler() != null) {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null) {
                skin = info.getLocationSkin();
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MC.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8,
                8, 8, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8,
                8, 8, 64.0F, 64.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    protected final EnumChatFormatting healthColor(EntityPlayer player) {
        if (player == null || player.getMaxHealth() <= 0.0F) {
            return EnumChatFormatting.WHITE;
        }
        float ratio = player.getHealth() / player.getMaxHealth();
        if (ratio > 0.75F) {
            return EnumChatFormatting.GREEN;
        }
        if (ratio > 0.5F) {
            return EnumChatFormatting.YELLOW;
        }
        if (ratio > 0.25F) {
            return EnumChatFormatting.GOLD;
        }
        return EnumChatFormatting.RED;
    }
}
