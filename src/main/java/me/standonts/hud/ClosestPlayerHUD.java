package me.standonts.hud;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClosestPlayerHUD extends AddonHud {

    private static final int LINE_HEIGHT = 10;
    private static final int TOTAL_WIDTH = 98;

    public ClosestPlayerHUD() {
        super(ExampleConfig.closestPlayerPosition);
    }

    @Override
    public void render(ScaledResolution resolution) {
        List<ClosestPlayer> players = getClosestPlayers();
        if (players.isEmpty()) {
            return;
        }
        int height = players.size() * LINE_HEIGHT;
        getPosition().updateAdjustedAbsolutePosition(resolution, TOTAL_WIDTH, height);
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        if (ExampleConfig.closestPlayerBackground) {
            drawRect(x - 2, y - 2, x + TOTAL_WIDTH, y + height + 1, 0x90000000);
        }
        for (int i = 0; i < players.size(); i++) {
            drawRow(players.get(i), x, y + i * LINE_HEIGHT);
        }
    }

    @Override
    public void renderDummy() {
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        if (ExampleConfig.closestPlayerBackground) {
            drawRect(x - 2, y - 2, x + TOTAL_WIDTH, y + 4 * LINE_HEIGHT + 1, 0x90000000);
        }
        char[] colors = {'c', 'a', 'e', '9'};
        for (int i = 0; i < colors.length; i++) {
            ClosestPlayer row = new ClosestPlayer(MC.thePlayer, colors[i], 25 + i * 5, i % 2 == 0 ? 3 : 1);
            drawRow(row, x, y + i * LINE_HEIGHT);
        }
    }

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        return getPosition().isEnabled() && MC.thePlayer != null && MC.theWorld != null
                && (MWEApi.Scoreboard.getScoreboardParser().isInMwGame()
                || MWEApi.Scoreboard.getScoreboardParser().isMWReplay());
    }

    private void drawRow(ClosestPlayer data, int x, int y) {
        drawPlayerHead(data.player, x, y + 1);
        String teamColor = "\u00A7" + data.teamColor;
        String count = data.teamCount > 1 ? "(" + data.teamCount + ")" : "";
        String distance = teamColor + (int) data.distance + count;
        MC.fontRendererObj.drawStringWithShadow(distance, x + 10, y + 1, 0xFFFFFF);

        if (data.player != null && MC.thePlayer != null) {
            drawDirectionArrow(data.player, x + 49, y + 1, data.teamColor);
            int heightDifference = (int) Math.round(data.player.posY - MC.thePlayer.posY);
            String heightText = heightDifference > 0
                    ? EnumChatFormatting.DARK_GREEN + "+" + heightDifference
                    : heightDifference < 0
                    ? EnumChatFormatting.DARK_RED + "-" + Math.abs(heightDifference)
                    : EnumChatFormatting.GRAY + "0";
            MC.fontRendererObj.drawStringWithShadow(heightText, x + 60, y + 1, 0xFFFFFF);

            int health = Math.max(0, (int) data.player.getHealth());
            String healthText = healthColor(data.player) + String.valueOf(health);
            int healthWidth = MC.fontRendererObj.getStringWidth(healthText);
            MC.fontRendererObj.drawStringWithShadow(healthText,
                    x + TOTAL_WIDTH - healthWidth - 2, y + 1, 0xFFFFFF);
        }
    }

    private List<ClosestPlayer> getClosestPlayers() {
        Map<Character, List<EntityPlayer>> playersByTeam = new HashMap<>();
        char ownTeam = MWEApi.Player.getPlayerInfo(MC.thePlayer).getPlayerTeamColor();
        for (EntityPlayer player : MC.theWorld.playerEntities) {
            if (player == MC.thePlayer || player.isInvisible() || player.isDead || isSpectator(player)) {
                continue;
            }
            char team = MWEApi.Player.getPlayerInfo(player).getPlayerTeamColor();
            if (team != '\0') {
                playersByTeam.computeIfAbsent(team, ignored -> new ArrayList<>()).add(player);
            }
        }

        ClosestPlayer friendly = null;
        List<ClosestPlayer> enemies = new ArrayList<>();
        for (Map.Entry<Character, List<EntityPlayer>> entry : playersByTeam.entrySet()) {
            EntityPlayer closest = entry.getValue().stream()
                    .min(Comparator.comparingDouble(MC.thePlayer::getDistanceSqToEntity))
                    .orElse(null);
            if (closest == null) {
                continue;
            }
            ClosestPlayer data = new ClosestPlayer(closest, entry.getKey(),
                    MC.thePlayer.getDistanceToEntity(closest), entry.getValue().size());
            if (entry.getKey() == ownTeam) {
                friendly = data;
            } else {
                enemies.add(data);
            }
        }
        enemies.sort(Comparator.comparingDouble(data -> data.distance));
        List<ClosestPlayer> result = new ArrayList<>();
        if (friendly != null) {
            result.add(friendly);
        }
        result.addAll(enemies);
        return result;
    }

    private boolean isSpectator(EntityPlayer player) {
        if (MC.getNetHandler() == null) {
            return false;
        }
        NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(player.getUniqueID());
        return info != null && info.getGameType() == GameType.SPECTATOR;
    }

    private void drawDirectionArrow(EntityPlayer target, int x, int y, char colorCode) {
        double dx = target.posX - MC.thePlayer.posX;
        double dz = target.posZ - MC.thePlayer.posZ;
        float rotation = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D
                - MC.thePlayer.rotationYaw);
        int rgb = colorForCode(colorCode);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F,
                (rgb & 255) / 255.0F, 1.0F);
        GlStateManager.translate(x + 3.0F, y + 4.0F, 0.0F);
        GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(-2.5F, 2.0F);
        GL11.glVertex2f(0.0F, -3.5F);
        GL11.glVertex2f(2.5F, 2.0F);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private int colorForCode(char code) {
        switch (Character.toLowerCase(code)) {
            case '1': return 0x0000AA;
            case '2': return 0x00AA00;
            case '3': return 0x00AAAA;
            case '4': return 0xAA0000;
            case '5': return 0xAA00AA;
            case '6': return 0xFFAA00;
            case '9': return 0x5555FF;
            case 'a': return 0x55FF55;
            case 'b': return 0x55FFFF;
            case 'c': return 0xFF5555;
            case 'd': return 0xFF55FF;
            case 'e': return 0xFFFF55;
            default: return 0xFFFFFF;
        }
    }

    private static final class ClosestPlayer {
        private final EntityPlayer player;
        private final char teamColor;
        private final double distance;
        private final int teamCount;

        private ClosestPlayer(EntityPlayer player, char teamColor, double distance, int teamCount) {
            this.player = player;
            this.teamColor = teamColor;
            this.distance = distance;
            this.teamCount = teamCount;
        }
    }
}
