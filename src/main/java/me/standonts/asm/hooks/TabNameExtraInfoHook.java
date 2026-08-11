package me.standonts.asm.hooks;

import fr.alexdoru.mwe.api.MWEApi;
import me.standonts.config.ExampleConfig;
import me.standonts.features.DiamondGearDetector;
import me.standonts.features.PhoenixDetector;
import me.standonts.features.PotionDetector;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;

public final class TabNameExtraInfoHook {

    private TabNameExtraInfoHook() {}

    public static String appendExtraInfo(String baseName, NetworkPlayerInfo playerInfo) {
        if (baseName == null || playerInfo == null || playerInfo.getGameProfile() == null
                || !MWEApi.Scoreboard.getScoreboardParser().isInMwGame()) {
            return baseName;
        }

        String username = playerInfo.getGameProfile().getName();
        String phoenixIcon = ExampleConfig.phoenixDetector
                ? PhoenixDetector.getResurrectionIcon(username) : null;
        int usedPots = ExampleConfig.showPotionUsedInTablist
                ? PotionDetector.getUsedPotsCount(username) : 0;
        String diamondIcons = ExampleConfig.extraDiamondGearIconsInTablist
                ? DiamondGearDetector.getExtraDiamondIcons(username) : "";
        if (phoenixIcon == null && usedPots <= 0 && diamondIcons.isEmpty()) {
            return baseName;
        }

        StringBuilder result = new StringBuilder(baseName);
        if (phoenixIcon != null) {
            result.append(phoenixIcon);
        }
        result.append(diamondIcons);
        if (usedPots > 0) {
            result.append(EnumChatFormatting.GRAY)
                    .append(" (")
                    .append(EnumChatFormatting.LIGHT_PURPLE)
                    .append(usedPots)
                    .append(EnumChatFormatting.GRAY)
                    .append(")");
        }
        return result.append(EnumChatFormatting.RESET).toString();
    }
}
