package me.standonts.asm.hooks;

import com.mojang.authlib.GameProfile;
import fr.alexdoru.mwe.api.MWEApi;
import me.standonts.config.ExampleConfig;
import me.standonts.features.DiamondGearDetector;
import me.standonts.features.PhoenixDetector;
import me.standonts.features.PotionDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.UUID;

public final class TabNameExtraInfoHook {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private TabNameExtraInfoHook() {}

    public static IChatComponent appendExtraInfo(IChatComponent baseName, GameProfile profile) {
        if (profile == null || profile.getId() == null || profile.getName() == null
                || !MWEApi.Scoreboard.getScoreboardParser().isInMwGame()) {
            return baseName;
        }

        UUID uuid = profile.getId();
        String phoenixIcon = ExampleConfig.phoenixDetector
                ? PhoenixDetector.getResurrectionIcon(uuid) : null;
        int usedPots = ExampleConfig.potionDetector && ExampleConfig.showPotionUsedInTablist
                ? PotionDetector.getUsedPotsCount(uuid) : 0;
        String diamondIcons = ExampleConfig.extraDiamondGearIconsInTablist
                ? DiamondGearDetector.getExtraDiamondIcons(uuid) : "";
        if (phoenixIcon == null && usedPots <= 0 && diamondIcons.isEmpty()) {
            return baseName;
        }

        StringBuilder result = new StringBuilder(getBaseName(baseName, profile));
        if (phoenixIcon != null) {
            result.append(" ").append(phoenixIcon);
        }
        if (usedPots > 0) {
            result.append(EnumChatFormatting.GRAY)
                    .append(" (")
                    .append(EnumChatFormatting.LIGHT_PURPLE)
                    .append(usedPots)
                    .append(EnumChatFormatting.GRAY)
                    .append(")");
        }
        if (!diamondIcons.isEmpty()) {
            result.append(EnumChatFormatting.GRAY).append(' ').append(diamondIcons);
        }
        return new ChatComponentText(result.append(EnumChatFormatting.RESET).toString());
    }

    private static String getBaseName(IChatComponent baseName, GameProfile profile) {
        if (baseName != null) {
            return baseName.getFormattedText();
        }
        ScorePlayerTeam team = MC.theWorld == null ? null
                : MC.theWorld.getScoreboard().getPlayersTeam(profile.getName());
        return ScorePlayerTeam.formatPlayerName(team, profile.getName());
    }
}
