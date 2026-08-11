package me.standonts.features;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.events.KillCounterEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class NameTagExtraInfo {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (!FeatureUtil.isInMegaWallsGame()) {
            return;
        }

        int usedPots = ExampleConfig.showPotionUsedOnNametags
                ? PotionDetector.getUsedPotsCount(event.username) : 0;
        int finals = ExampleConfig.showFinalKillsOnNametags
                ? MWEApi.FinalKills.getKillsOfPlayer(event.username) : 0;
        String phoenixIcon = ExampleConfig.phoenixDetector
                ? PhoenixDetector.getResurrectionIcon(event.username) : null;

        StringBuilder suffix = new StringBuilder();

        if (usedPots > 0 || finals > 0 || phoenixIcon != null) {
            suffix.append(EnumChatFormatting.GRAY).append(" [");
            boolean hasValue = false;
            if (usedPots > 0) {
                suffix.append(EnumChatFormatting.LIGHT_PURPLE).append(usedPots);
                hasValue = true;
            }
            if (finals > 0) {
                if (hasValue) {
                    suffix.append(EnumChatFormatting.GRAY).append(" | ");
                }
                suffix.append(EnumChatFormatting.GOLD).append(finals);
                hasValue = true;
            }
            if (phoenixIcon != null) {
                if (hasValue) {
                    suffix.append(EnumChatFormatting.GRAY).append(" | ");
                }
                suffix.append(phoenixIcon);
            }
            suffix.append(EnumChatFormatting.GRAY).append("]");
        }

        if (suffix.length() > 0) {
            String baseName = event.displayname == null ? event.username : event.displayname;
            event.displayname = baseName + suffix + EnumChatFormatting.RESET;
        }
    }

    @SubscribeEvent
    public void onKillCounter(KillCounterEvent event) {
        refresh(event.victim);
        if (event instanceof KillCounterEvent.KillEvent) {
            refresh(((KillCounterEvent.KillEvent) event).killer);
        }
    }

    private void refresh(String name) {
        EntityPlayer player = FeatureUtil.findPlayer(name);
        if (player != null) {
            player.refreshDisplayName();
        }
    }
}
