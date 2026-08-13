package me.standonts.asm.hooks;

import fr.alexdoru.mwe.api.MWEApi;
import me.standonts.config.ExampleConfig;

public final class FinalKillCounterHook {

    private FinalKillCounterHook() {}

    public static boolean allowMessageMatch(boolean matchesKillMessageStart) {
        return matchesKillMessageStart && (!ExampleConfig.onlyDMCounts
                || MWEApi.Scoreboard.getScoreboardParser().isDeathmatch());
    }
}
