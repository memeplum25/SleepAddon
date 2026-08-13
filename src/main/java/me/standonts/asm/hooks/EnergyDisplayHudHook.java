package me.standonts.asm.hooks;

import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

public final class EnergyDisplayHudHook {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private EnergyDisplayHudHook() {}

    public static String appendMeleeHits(String energyText) {
        if (!ExampleConfig.showMeleeHitsInEnergyHud || MC.thePlayer == null) {
            return energyText;
        }
        int energy = MC.thePlayer.experienceLevel;
        if (energy <= 0 || energy >= 100) {
            return energyText;
        }

        int energyPerHit = getEnergyPerHit(resolveClass(MC.thePlayer));
        if (energyPerHit <= 0) {
            return energyText;
        }
        int hits = (int) Math.ceil((100.0D - energy) / energyPerHit);
        return appendHits(energyText, hits);
    }

    public static String appendDummyMeleeHits(String energyText) {
        return ExampleConfig.showMeleeHitsInEnergyHud
                ? appendHits(energyText, 5) : energyText;
    }

    private static String appendHits(String energyText, int hits) {
        return energyText + EnumChatFormatting.GRAY + " ("
                + EnumChatFormatting.GOLD + hits
                + EnumChatFormatting.GRAY + ")";
    }

    private static MWClass resolveClass(EntityPlayer player) {
        MWClass mwClass = MWEApi.Player.getPlayerInfo(player).getMWClass();
        return mwClass == null ? MWClass.ofPlayer(player.getName()) : mwClass;
    }

    private static int getEnergyPerHit(MWClass mwClass) {
        if (mwClass == null) {
            return 0;
        }
        switch (mwClass) {
            case COW:
            case HEROBRINE:
                return 25;
            case ARCANIST:
                return 34;
            case CREEPER:
                return 30;
            case ENDERMAN:
            case SKELETON:
                return 20;
            case SHARK:
                return 18;
            case RENEGADE:
                return 13;
            case ZOMBIE:
            case PIRATE:
            case ANGEL:
            case DRAGON:
                return 12;
            case DREADLORD:
            case GOLEM:
            case PIGMAN:
            case SHAMAN:
            case SQUID:
            case SHEEP:
            case WEREWOLF:
            case ASSASSIN:
            case MOLEMAN:
                return 10;
            case BLAZE:
            case SPIDER:
            case PHOENIX:
            case SNOWMAN:
                return 8;
            case HUNTER:
            case AUTOMATON:
                return 4;
            default:
                return 0;
        }
    }
}
