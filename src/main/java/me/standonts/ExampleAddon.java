package me.standonts;

import me.standonts.config.ExampleConfig;
import me.standonts.features.Denick;
import me.standonts.features.DiamondGearDetector;
import me.standonts.features.GhostBlockFix;
import me.standonts.features.NameTagExtraInfo;
import me.standonts.features.ItemTags;
import me.standonts.features.PhoenixDetector;
import me.standonts.features.PotionDetector;
import me.standonts.hud.ClosestPlayerHUD;
import me.standonts.hud.ClosestStrengthHUD;
import me.standonts.hud.LeapModeHUD;
import me.standonts.hud.MiniScoreboardHUD;
import me.standonts.hud.SkillSlayHUD;
import fr.alexdoru.mwe.api.IMWEAddon;
import fr.alexdoru.mwe.api.MWEApi;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ExampleAddon implements IMWEAddon {

    @Override
    public String name() {
        return BuildConfig.ADDON_NAME;
    }

    @Override
    public String targetVersion() {
        return BuildConfig.TARGET_MWE_VERSION;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MWEApi.Config.registerConfig(ExampleConfig.class);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new PotionDetector());
        MinecraftForge.EVENT_BUS.register(new PhoenixDetector());
        MinecraftForge.EVENT_BUS.register(new NameTagExtraInfo());
        MinecraftForge.EVENT_BUS.register(new ItemTags());
        MinecraftForge.EVENT_BUS.register(new Denick());
        MinecraftForge.EVENT_BUS.register(new DiamondGearDetector());
        MinecraftForge.EVENT_BUS.register(new GhostBlockFix());
        ClientCommandHandler.instance.registerCommand(new Denick.CommandDenick());

        ClosestStrengthHUD closestStrengthHUD = new ClosestStrengthHUD();
        MinecraftForge.EVENT_BUS.register(closestStrengthHUD);
        MWEApi.Hud.registerHUD(closestStrengthHUD);
        MWEApi.Hud.registerHUD(new MiniScoreboardHUD());
        MWEApi.Hud.registerHUD(new SkillSlayHUD());
        MWEApi.Hud.registerHUD(new ClosestPlayerHUD());
        LeapModeHUD leapModeHUD = new LeapModeHUD();
        MinecraftForge.EVENT_BUS.register(leapModeHUD);
        MWEApi.Hud.registerHUD(leapModeHUD);

    }

    @Override
    public void postInit(FMLPostInitializationEvent fmlPostInitializationEvent) {

    }

}
