package me.standonts.features;

import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.data.PlayerDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;
import java.util.UUID;

final class FeatureUtil {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private FeatureUtil() {}

    static boolean isInMegaWallsGame() {
        return MWEApi.Scoreboard.getScoreboardParser().isInMwGame();
    }

    static EntityPlayer findPlayer(UUID uuid) {
        if (uuid == null || MC.theWorld == null) {
            return null;
        }
        List<EntityPlayer> players = MC.theWorld.playerEntities;
        for (int i = players.size() - 1; i >= 0; i--) {
            EntityPlayer player = players.get(i);
            if (uuid.equals(player.getUniqueID())) {
                return player;
            }
        }
        return null;
    }

    static EntityPlayer findPlayer(String name) {
        if (name == null || MC.theWorld == null) {
            return null;
        }
        List<EntityPlayer> players = MC.theWorld.playerEntities;
        for (int i = players.size() - 1; i >= 0; i--) {
            EntityPlayer player = players.get(i);
            if (name.equalsIgnoreCase(player.getName())) {
                return player;
            }
        }
        return null;
    }

    static void refreshName(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (MC.getNetHandler() != null) {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(uuid);
            if (info != null) {
                PlayerDataManager.updatePlayerDataAndEntityData(info);
                return;
            }
        }
        EntityPlayer player = findPlayer(uuid);
        if (player != null) {
            player.refreshDisplayName();
        }
    }

    static void refreshAllNames() {
        if (MC.theWorld != null && MC.getNetHandler() != null) {
            PlayerDataManager.refreshAllNamesInWorld();
        }
    }

    static String formattedName(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) {
            return "";
        }
        return ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), info.getGameProfile().getName());
    }

    static void sendMessage(String feature, EnumChatFormatting color, String message) {
        MWEApi.Chat.addChatMessage(EnumChatFormatting.DARK_GRAY + "[" + color + feature
                + EnumChatFormatting.DARK_GRAY + "] " + EnumChatFormatting.RESET + message);
    }
}
