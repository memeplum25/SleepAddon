package me.standonts;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
public class AddonBootstrap implements IFMLLoadingPlugin {

    private static final String ADDONS_KEY = "mwe.addons";
    private static final String TRANSFORMERS_KEY = "mwe.transformers";

    public AddonBootstrap() {
        // TODO register your addon main class here
        appendToBlackboard(ADDONS_KEY, "me.standonts.ExampleAddon");

        appendToBlackboard(TRANSFORMERS_KEY,
                "me.standonts.asm.BaseLocationHudTransformer",
                "me.standonts.asm.FinalKillCounterTransformer",
                "me.standonts.asm.GhostBlockChangeTransformer",
                "me.standonts.asm.NetworkManagerSendPacketTransformer",
                "me.standonts.asm.EnergyDisplayHudTransformer"
        );
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void appendToBlackboard(String key, String... classnames) {
        Object o = Launch.blackboard.computeIfAbsent(key, (k) -> new ArrayList<>());
        if (o instanceof List) {
            List list = (List) o;
            for (String classname : classnames) {
                if (!list.contains(classname)) {
                    list.add(classname);
                }
            }
        }
    }
}
