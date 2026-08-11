package me.standonts.asm.hooks;

import com.mojang.authlib.GameProfile;
import fr.alexdoru.configlib.api.IRenderer;
import fr.alexdoru.configlib.api.RendererPosition;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.data.NameFormatter;
import fr.alexdoru.mwe.utils.ColorUtil;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SquadHealthHudHook {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int ROW_HEIGHT = 8;
    private static final int HEAD_SIZE = 7;
    private static final int HEAD_COLUMN_WIDTH = 9;
    private static final int ARROW_COLUMN_WIDTH = 10;
    private static final int OUTER_BACKGROUND = new Color(14, 14, 14, 100).getRGB();
    private static final int ROW_BACKGROUND = new Color(8, 8, 8, 120).getRGB();
    private static final Map<String, LastPosition> LAST_POSITIONS = new HashMap<>();

    private static World trackedWorld;

    private SquadHealthHudHook() {}

    public static void render(IRenderer hud, ScaledResolution resolution) {
        if (MC.thePlayer == null || MC.theWorld == null || MC.getNetHandler() == null) {
            return;
        }

        Set<String> squadNames = new HashSet<>(MWEApi.Squad.getSquadMap().keySet());
        updatePositions(squadNames);

        List<NetworkPlayerInfo> playerInfos = collectPlayerInfos(squadNames);
        if (playerInfos.size() <= 1) {
            return;
        }

        sortPlayerInfos(playerInfos);
        List<Row> rows = buildRows(playerInfos);
        Dimensions dimensions = measure(rows);

        RendererPosition position = hud.getPosition();
        position.updateAdjustedAbsolutePosition(resolution, dimensions.width, dimensions.height);
        drawRows(position.getAbsoluteRenderX(), position.getAbsoluteRenderY(), dimensions, rows);
    }

    public static void renderDummy(IRenderer hud) {
        if (MC.fontRendererObj == null) {
            return;
        }

        String ownName = MC.thePlayer == null ? "Player" : MC.thePlayer.getName();
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            boolean self = i == 0;
            String name = EnumChatFormatting.GREEN + ownName;
            String finals = EnumChatFormatting.GOLD + " " + (3 + (i * 3) % 5);
            String distance = self ? "" : sampleDistance(i);
            EnumChatFormatting healthColor = MC.thePlayer == null
                    ? EnumChatFormatting.YELLOW
                    : ColorUtil.getColoredHP(EnumChatFormatting.YELLOW, 20 - i * 2);
            String health = self ? "" : healthColor + " " + (20 - i * 2);
            rows.add(new Row(null, null, name, finals, distance, health,
                    !self, i * 70.0F, i % 2 == 0));
        }

        Dimensions dimensions = measure(rows);
        RendererPosition position = hud.getPosition();
        drawRows(position.getAbsoluteRenderX(), position.getAbsoluteRenderY(), dimensions, rows);
    }

    public static boolean isEnabled(IRenderer hud, long ignoredCurrentTimeMillis) {
        return hud.getPosition().isEnabled() && MWEApi.Squad.getSquadMap().size() > 1;
    }

    private static void updatePositions(Set<String> squadNames) {
        if (trackedWorld != MC.theWorld) {
            trackedWorld = MC.theWorld;
            LAST_POSITIONS.clear();
        }

        LAST_POSITIONS.keySet().retainAll(squadNames);
        for (String name : squadNames) {
            EntityPlayer player = MC.theWorld.getPlayerEntityByName(name);
            if (player != null) {
                LAST_POSITIONS.put(name, new LastPosition(player.posX, player.posY, player.posZ));
            }
        }
    }

    private static List<NetworkPlayerInfo> collectPlayerInfos(Set<String> squadNames) {
        List<NetworkPlayerInfo> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (String name : squadNames) {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(name);
            if (info != null && seen.add(info.getGameProfile().getId())) {
                result.add(info);
            }
        }
        return result;
    }

    private static void sortPlayerInfos(List<NetworkPlayerInfo> playerInfos) {
        final UUID selfId = MC.thePlayer.getUniqueID();
        Collections.sort(playerInfos, new Comparator<NetworkPlayerInfo>() {
            @Override
            public int compare(NetworkPlayerInfo left, NetworkPlayerInfo right) {
                if (ExampleConfig.squadHudSelfFirst) {
                    boolean leftIsSelf = selfId.equals(left.getGameProfile().getId());
                    boolean rightIsSelf = selfId.equals(right.getGameProfile().getId());
                    if (leftIsSelf != rightIsSelf) {
                        return leftIsSelf ? -1 : 1;
                    }
                }

                String leftName = left.getGameProfile().getName();
                String rightName = right.getGameProfile().getName();
                int insensitive = leftName.compareToIgnoreCase(rightName);
                return insensitive != 0 ? insensitive : leftName.compareTo(rightName);
            }
        });
    }

    private static List<Row> buildRows(List<NetworkPlayerInfo> playerInfos) {
        List<Row> rows = new ArrayList<>(playerInfos.size());
        UUID selfId = MC.thePlayer.getUniqueID();

        for (NetworkPlayerInfo info : playerInfos) {
            GameProfile profile = info.getGameProfile();
            String playerName = profile.getName();
            boolean self = selfId.equals(profile.getId());
            EntityPlayer entity = MC.theWorld.getPlayerEntityByUUID(profile.getId());
            String formattedName = NameFormatter.getFormattedNameWithoutIcons(info);

            int finalKills = MWEApi.FinalKills.getKillsOfPlayer(playerName);
            String finals = finalKills == 0 ? "" : EnumChatFormatting.GOLD + " " + finalKills;
            int health = getHealth(info, entity);
            String healthText = self ? "" : ColorUtil.getColoredHP(EnumChatFormatting.YELLOW, health)
                    + " " + health;

            LastPosition lastPosition = LAST_POSITIONS.get(playerName);
            String distance = "";
            boolean hasArrow = !self && lastPosition != null;
            boolean realTime = entity != null;
            float arrowRotation = 0.0F;
            if (hasArrow) {
                distance = getDistanceText(lastPosition, realTime);
                arrowRotation = getArrowRotation(lastPosition);
            }

            rows.add(new Row(info, entity, formattedName, finals, distance, healthText,
                    hasArrow, arrowRotation, realTime));
        }

        return rows;
    }

    private static int getHealth(NetworkPlayerInfo info, EntityPlayer entity) {
        if (entity != null) {
            return (int) entity.getHealth();
        }

        try {
            Scoreboard scoreboard = MC.theWorld.getScoreboard();
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(0);
            if (objective != null
                    && objective.getRenderType() != IScoreObjectiveCriteria.EnumRenderType.HEARTS
                    && info.getGameType() != WorldSettings.GameType.SPECTATOR) {
                return scoreboard.getValueFromObjective(info.getGameProfile().getName(), objective).getScorePoints();
            }
        } catch (RuntimeException ignored) {
            // Scoreboards can be replaced while a packet is being handled.
        }
        return 0;
    }

    private static String getDistanceText(LastPosition position, boolean realTime) {
        double dx = position.x - MC.thePlayer.posX;
        double dy = position.y - MC.thePlayer.posY;
        double dz = position.z - MC.thePlayer.posZ;
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        int verticalDifference = (int) Math.round(dy);
        String sign = verticalDifference > 0 ? "+" : verticalDifference < 0 ? "-" : "";

        EnumChatFormatting frameColor = realTime ? EnumChatFormatting.LIGHT_PURPLE : EnumChatFormatting.GRAY;
        EnumChatFormatting heightColor;
        if (!realTime || verticalDifference == 0) {
            heightColor = EnumChatFormatting.GRAY;
        } else {
            heightColor = verticalDifference > 0 ? EnumChatFormatting.DARK_GREEN : EnumChatFormatting.DARK_RED;
        }

        return " " + frameColor + "(" + distance + "m " + heightColor + sign
                + Math.abs(verticalDifference) + frameColor + ")";
    }

    private static float getArrowRotation(LastPosition position) {
        double dx = position.x - MC.thePlayer.posX;
        double dz = position.z - MC.thePlayer.posZ;
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        return (float) (targetYaw - MC.thePlayer.rotationYaw);
    }

    private static Dimensions measure(List<Row> rows) {
        int maxNameWidth = 0;
        int maxFinalsWidth = 0;
        int maxDistanceWidth = MC.fontRendererObj.getStringWidth(" (900m +100)");
        int maxHealthWidth = MC.fontRendererObj.getStringWidth(" 20");

        for (Row row : rows) {
            maxNameWidth = Math.max(maxNameWidth, MC.fontRendererObj.getStringWidth(row.formattedName));
            maxFinalsWidth = Math.max(maxFinalsWidth, MC.fontRendererObj.getStringWidth(row.finals));
            maxDistanceWidth = Math.max(maxDistanceWidth, MC.fontRendererObj.getStringWidth(row.distance));
            maxHealthWidth = Math.max(maxHealthWidth, MC.fontRendererObj.getStringWidth(row.health));
        }

        int width = 4 + HEAD_COLUMN_WIDTH + maxNameWidth + maxFinalsWidth
                + maxDistanceWidth + ARROW_COLUMN_WIDTH + maxHealthWidth;
        int height = rows.size() * ROW_HEIGHT + 2;
        return new Dimensions(width, height, maxNameWidth, maxFinalsWidth, maxHealthWidth);
    }

    private static void drawRows(int hudX, int hudY, Dimensions dimensions, List<Row> rows) {
        Gui.drawRect(hudX, hudY - 1, hudX + dimensions.width, hudY + dimensions.height,
                OUTER_BACKGROUND);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowY = hudY + 1 + i * ROW_HEIGHT;
            Gui.drawRect(hudX + 1, rowY - 1, hudX + dimensions.width - 1,
                    rowY + ROW_HEIGHT, ROW_BACKGROUND);

            int cursorX = hudX + 2;
            drawHead(row, cursorX, rowY);
            cursorX += HEAD_COLUMN_WIDTH;

            MC.fontRendererObj.drawString(row.formattedName, cursorX, rowY, 0xFFFFFF);
            cursorX += dimensions.nameWidth;
            if (!row.finals.isEmpty()) {
                MC.fontRendererObj.drawString(row.finals, cursorX, rowY, 0xFFFFFF);
            }
            cursorX += dimensions.finalsWidth;
            if (!row.distance.isEmpty()) {
                MC.fontRendererObj.drawString(row.distance, cursorX, rowY, 0xFFFFFF);
            }

            int healthRight = hudX + dimensions.width - 2;
            int arrowX = healthRight - dimensions.healthWidth - ARROW_COLUMN_WIDTH + 1;
            if (row.hasArrow) {
                drawDirectionArrow(arrowX, rowY - 1, row.arrowRotation, row.realTime);
            }
            if (!row.health.isEmpty()) {
                int healthX = healthRight - MC.fontRendererObj.getStringWidth(row.health);
                MC.fontRendererObj.drawString(row.health, healthX, rowY, 0xFFFFFF);
            }
        }
    }

    private static void drawHead(Row row, int x, int y) {
        ResourceLocation skin = row.info == null
                ? DefaultPlayerSkin.getDefaultSkinLegacy()
                : row.info.getLocationSkin();
        boolean hatLayer = row.entity == null || row.entity.isWearing(EnumPlayerModelParts.HAT);

        GlStateManager.pushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MC.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8,
                HEAD_SIZE, HEAD_SIZE, 64.0F, 64.0F);
        if (hatLayer) {
            Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8,
                    HEAD_SIZE, HEAD_SIZE, 64.0F, 64.0F);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

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

    private static String sampleDistance(int index) {
        int distance = 32 + index * 17;
        int vertical = index * 4;
        return " " + EnumChatFormatting.LIGHT_PURPLE + "(" + distance + "m "
                + EnumChatFormatting.DARK_GREEN + "+" + vertical
                + EnumChatFormatting.LIGHT_PURPLE + ")";
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

    private static final class Row {
        private final NetworkPlayerInfo info;
        private final EntityPlayer entity;
        private final String formattedName;
        private final String finals;
        private final String distance;
        private final String health;
        private final boolean hasArrow;
        private final float arrowRotation;
        private final boolean realTime;

        private Row(NetworkPlayerInfo info, EntityPlayer entity, String formattedName,
                    String finals, String distance, String health, boolean hasArrow,
                    float arrowRotation, boolean realTime) {
            this.info = info;
            this.entity = entity;
            this.formattedName = formattedName;
            this.finals = finals;
            this.distance = distance;
            this.health = health;
            this.hasArrow = hasArrow;
            this.arrowRotation = arrowRotation;
            this.realTime = realTime;
        }
    }

    private static final class Dimensions {
        private final int width;
        private final int height;
        private final int nameWidth;
        private final int finalsWidth;
        private final int healthWidth;

        private Dimensions(int width, int height, int nameWidth, int finalsWidth,
                           int healthWidth) {
            this.width = width;
            this.height = height;
            this.nameWidth = nameWidth;
            this.finalsWidth = finalsWidth;
            this.healthWidth = healthWidth;
        }
    }
}
