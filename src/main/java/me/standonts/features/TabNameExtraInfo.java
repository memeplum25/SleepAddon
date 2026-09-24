package me.standonts.features;

import com.mojang.authlib.GameProfile;
import fr.alexdoru.mwe.api.ITabNameModifier;
import me.standonts.config.ExampleConfig;
import net.minecraft.util.EnumChatFormatting;

import java.util.UUID;

/**
 * Appends the addon's extra info (phoenix icon, tracked potions, extra diamond gear)
 * to the player names in the tablist.
 * <p>
 * Uses MWE's tab name modifier API, this replaces the asm hook that used to
 * inject the info inside of MWE's {@code PlayerDataManager}.
 */
public final class TabNameExtraInfo implements ITabNameModifier {

    private static final int PRIORITY = 0;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean shouldModifyName(GameProfile gameProfile) {
        return !buildExtraInfo(gameProfile).isEmpty();
    }

    @Override
    public void modifyTabname(GameProfile gameProfile, StringBuilder prefix, StringBuilder suffix) {
        suffix.append(buildExtraInfo(gameProfile));
    }

    private String buildExtraInfo(GameProfile profile) {
        if (profile == null || profile.getId() == null || profile.getName() == null
                || !FeatureUtil.isInMegaWallsGame()) {
            return "";
        }

        UUID uuid = profile.getId();
        String phoenixIcon = ExampleConfig.phoenixDetector
                ? PhoenixDetector.getResurrectionIcon(uuid) : null;
        int usedPots = ExampleConfig.potionDetector && ExampleConfig.showPotionUsedInTablist
                ? PotionDetector.getUsedPotsCount(uuid) : 0;
        String diamondIcons = ExampleConfig.extraDiamondGearIconsInTablist
                ? DiamondGearDetector.getExtraDiamondIcons(uuid) : "";
        if (phoenixIcon == null && usedPots <= 0
                && (diamondIcons == null || diamondIcons.isEmpty())) {
            return "";
        }

        StringBuilder extra = new StringBuilder();
        if (phoenixIcon != null) {
            extra.append(' ').append(phoenixIcon);
        }
        if (usedPots > 0) {
            extra.append(EnumChatFormatting.GRAY).append(" (")
                    .append(EnumChatFormatting.LIGHT_PURPLE).append(usedPots)
                    .append(EnumChatFormatting.GRAY).append(')');
        }
        if (diamondIcons != null && !diamondIcons.isEmpty()) {
            extra.append(EnumChatFormatting.GRAY).append(' ').append(diamondIcons);
        }
        return extra.append(EnumChatFormatting.RESET).toString();
    }

}
