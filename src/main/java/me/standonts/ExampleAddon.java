package me.standonts;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.IMWEAddon;
import fr.alexdoru.mwe.api.MWEApi;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ExampleAddon implements IMWEAddon {

    private static boolean initialized;

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
        if (initialized) {
            return;
        }
        initialized = true;
        new AddonRuntime().register();
    }

    @Override
    public void postInit(FMLPostInitializationEvent fmlPostInitializationEvent) {
    }

}
