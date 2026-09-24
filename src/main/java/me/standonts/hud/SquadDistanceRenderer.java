package me.standonts.hud;

import fr.alexdoru.mwe.api.ISquadInfoRenderer;
import fr.alexdoru.mwe.api.MWEApi;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adds a column with the distance and a direction arrow to every squadmate on MWE's squad HUD.
 * The last known position of each squadmate is remembered so that squadmates outside of the
 * render distance keep an indicator pointing at them.
 */
public final class SquadDistanceRenderer implements ISquadInfoRenderer {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int ARROW_WIDTH = 10;
    private static final String SAMPLE_DISTANCE = " (900m +100)";

    private final Map<UUID, LastPosition> lastPositions = new HashMap<>();

    private World trackedWorld;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MC.theWorld == null || MC.getNetHandler() == null) {
            return;
        }
        if (trackedWorld != MC.theWorld) {
            trackedWorld = MC.theWorld;
            lastPositions.clear();
        }
        final Set<UUID> squadIds = new HashSet<>();
        for (String playername : MWEApi.Squad.getSquadMap().keySet()) {
            final NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(playername);
            if (info != null) {
                squadIds.add(info.getGameProfile().getId());
            }
        }
        lastPositions.keySet().retainAll(squadIds);
        for (UUID id : squadIds) {
            final EntityPlayer player = MC.theWorld.getPlayerEntityByUUID(id);
            if (player != null) {
                lastPositions.put(id, new LastPosition(player.posX, player.posY, player.posZ));
            }
        }
    }

    @Override
    public int getWidth(NetworkPlayerInfo netInfo, EntityPlayer entityPlayer) {
        final String distance = distanceText(netInfo, entityPlayer);
        if (distance == null) {
            return 0;
        }
        return Math.max(MC.fontRendererObj.getStringWidth(SAMPLE_DISTANCE),
                MC.fontRendererObj.getStringWidth(distance)) + ARROW_WIDTH;
    }

    @Override
    public void render(NetworkPlayerInfo netInfo, EntityPlayer entityPlayer, int x, int y, int reservedWidth) {
        final String distance = distanceText(netInfo, entityPlayer);
        if (distance == null) {
            return;
        }
        MC.fontRendererObj.drawStringWithShadow(distance, x, y, 0xFFFFFF);
        final LastPosition position = lastPositions.get(netInfo.getGameProfile().getId());
        drawDirectionArrow(x + reservedWidth - ARROW_WIDTH + 1, y - 1,
                getArrowRotation(position), entityPlayer != null);
    }

    /** Distance of that squadmate, null when nothing has to be drawn on that line */
    private String distanceText(NetworkPlayerInfo netInfo, EntityPlayer entityPlayer) {
        if (!ExampleConfig.squadHudDistance || MC.thePlayer == null || isSelf(netInfo)) {
            return null;
        }
        final LastPosition position = lastPositions.get(netInfo.getGameProfile().getId());
        if (position == null) {
            return null;
        }
        final boolean realTime = entityPlayer != null;
        final double dx = position.x - MC.thePlayer.posX;
        final double dz = position.z - MC.thePlayer.posZ;
        final int distance = (int) Math.sqrt(dx * dx + dz * dz);
        final int height = (int) Math.round(position.y - MC.thePlayer.posY);
        final EnumChatFormatting frameColor = realTime ? EnumChatFormatting.LIGHT_PURPLE : EnumChatFormatting.GRAY;
        final EnumChatFormatting heightColor;
        if (!realTime || height == 0) {
            heightColor = EnumChatFormatting.GRAY;
        } else {
            heightColor = height > 0 ? EnumChatFormatting.DARK_GREEN : EnumChatFormatting.DARK_RED;
        }
        final String sign = height > 0 ? "+" : height < 0 ? "-" : "";
        return " " + frameColor + "(" + distance + "m " + heightColor + sign
                + Math.abs(height) + frameColor + ")";
    }

    private static boolean isSelf(NetworkPlayerInfo netInfo) {
        return MC.thePlayer != null && MC.thePlayer.getUniqueID().equals(netInfo.getGameProfile().getId());
    }

    private static float getArrowRotation(LastPosition position) {
        final double dx = position.x - MC.thePlayer.posX;
        final double dz = position.z - MC.thePlayer.posZ;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D - MC.thePlayer.rotationYaw);
    }

    /** Magenta arrow while the squadmate is loaded, gray while only the last known position is used */
    private static void drawDirectionArrow(int x, int y, float rotation, boolean realTime) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        if (realTime) {
            GL11.glColor4f(1.0F, 0.33F, 1.0F, 1.0F);
        } else {
            GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
        }

        GlStateManager.translate(x + 4.0F, y + 4.0F, 0.0F);
        GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
        GL11.glLineWidth(1.5F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(-2.5F, 2.5F);
        GL11.glVertex2f(0.0F, -3.5F);
        GL11.glVertex2f(2.5F, 2.5F);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static final class LastPosition {

        private final double x;
        private final double y;
        private final double z;

        private LastPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

}