package me.standonts;

import me.standonts.features.AutoCraft;
import me.standonts.features.Denick;
import me.standonts.features.DiamondGearDetector;
import me.standonts.features.GhostBlockFix;
import me.standonts.features.ItemTags;
import me.standonts.features.NameTagExtraInfo;
import me.standonts.features.PhoenixDetector;
import me.standonts.features.PotionDetector;
import me.standonts.features.TabNameExtraInfo;
import me.standonts.hud.ClosestPlayerHUD;
import me.standonts.hud.ClosestStrengthHUD;
import me.standonts.hud.LeapModeHUD;
import me.standonts.hud.MiniScoreboardHUD;
import me.standonts.hud.SkillSlayHUD;
import me.standonts.hud.SquadDistanceRenderer;
import fr.alexdoru.configlib.api.IRenderer;
import fr.alexdoru.mwe.api.MWEApi;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

/** Owns addon runtime objects so registration has one clear lifecycle boundary. */
final class AddonRuntime {
    void register() {
        registerEventFeatures();
        MWEApi.Names.registerTabNameModifier(new TabNameExtraInfo());
        ClientCommandHandler.instance.registerCommand(new Denick.CommandDenick());
        registerHud(new ClosestStrengthHUD(), true);
        registerHud(new MiniScoreboardHUD(), true);
        registerHud(new LeapModeHUD(), true);
        registerHud(new SkillSlayHUD(), false);
        registerHud(new ClosestPlayerHUD(), false);
        SquadDistanceRenderer squad = new SquadDistanceRenderer();
        MinecraftForge.EVENT_BUS.register(squad);
        MWEApi.Hud.registerSquadHUDExtraRenderer(squad);
    }

    private void registerEventFeatures() {
        MinecraftForge.EVENT_BUS.register(new PotionDetector());
        MinecraftForge.EVENT_BUS.register(new PhoenixDetector());
        MinecraftForge.EVENT_BUS.register(new NameTagExtraInfo());
        MinecraftForge.EVENT_BUS.register(new ItemTags());
        MinecraftForge.EVENT_BUS.register(new Denick());
        MinecraftForge.EVENT_BUS.register(new DiamondGearDetector());
        MinecraftForge.EVENT_BUS.register(new GhostBlockFix());
        MinecraftForge.EVENT_BUS.register(new AutoCraft());
    }

    private void registerHud(IRenderer hud, boolean listensToEvents) {
        if (listensToEvents) MinecraftForge.EVENT_BUS.register(hud);
        MWEApi.Hud.registerHUD(hud);
    }
}
