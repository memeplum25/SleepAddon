package me.standonts.features;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import fr.alexdoru.mwe.api.events.MegaWallsGameEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PotionDetector {

    private static final int DETECT_COOLDOWN_TICKS = 30;
    private static final int SOFT_CONFIRM_WINDOW_TICKS = 8;
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<UUID, PotionState> STATES = new HashMap<>();
    private static final Map<String, UUID> UUIDS_BY_NAME = new HashMap<>();
    private static final Map<MWClass, PotionData> POTIONS = createPotionData();

    private String gameId;

    public static int getUsedPotsCount(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return 0;
        }
        UUID uuid = UUIDS_BY_NAME.get(playerName.toLowerCase(Locale.ROOT));
        if (uuid == null && MC.getNetHandler() != null) {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(playerName);
            if (info != null && info.getGameProfile() != null) {
                uuid = info.getGameProfile().getId();
                cacheName(playerName, uuid);
            }
        }
        return getUsedPotsCount(uuid);
    }

    public static int getUsedPotsCount(UUID uuid) {
        PotionState state = uuid == null ? null : STATES.get(uuid);
        return state == null ? 0 : state.usedPots;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ExampleConfig.potionDetector
                || MC.theWorld == null || MC.getNetHandler() == null
                || !FeatureUtil.isInMegaWallsGame()
                || !MWEApi.Scoreboard.getScoreboardParser().isDeathmatch()) {
            return;
        }
        detectPotionUsage();
    }

    @SubscribeEvent
    public void onGameEvent(MegaWallsGameEvent event) {
        if (event.type == MegaWallsGameEvent.Type.CONNECT) {
            String currentGameId = MWEApi.Scoreboard.getScoreboardParser().getServerID();
            if (gameId == null || !gameId.equals(currentGameId)) {
                reset();
                gameId = currentGameId;
            }
        } else if (event.type == MegaWallsGameEvent.Type.GAME_START
                || event.type == MegaWallsGameEvent.Type.GAME_END
                || event.type == MegaWallsGameEvent.Type.DISCONNECT) {
            reset();
            if (event.type == MegaWallsGameEvent.Type.DISCONNECT) {
                gameId = null;
            }
        }
    }

    private void detectPotionUsage() {
        int currentTick = (int) (MC.theWorld.getTotalWorldTime() & Integer.MAX_VALUE);
        for (NetworkPlayerInfo info : MC.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) {
                continue;
            }
            UUID uuid = info.getGameProfile().getId();
            String name = info.getGameProfile().getName();
            cacheName(name, uuid);
            if (info.getGameType() == GameType.SPECTATOR) {
                STATES.remove(uuid);
                continue;
            }

            PotionData potion = POTIONS.get(resolveClass(info));
            if (potion == null || potion.count == 0) {
                continue;
            }
            int health = resolveHealth(info);
            PotionState state = STATES.computeIfAbsent(uuid, ignored -> new PotionState());
            if (state.lastHealth > 0 && health > state.lastHealth
                    && shouldCount(state, health - state.lastHealth, potion, currentTick)) {
                recordUse(info, state, potion, currentTick);
            }
            state.lastHealth = health;
        }
    }

    private MWClass resolveClass(NetworkPlayerInfo info) {
        EntityPlayer entity = FeatureUtil.findPlayer(info.getGameProfile().getId());
        if (entity != null) {
            MWClass mwClass = MWEApi.Player.getPlayerInfo(entity).getMWClass();
            if (mwClass != null) {
                return mwClass;
            }
        }
        return MWClass.ofPlayer(info.getGameProfile().getId());
    }

    private int resolveHealth(NetworkPlayerInfo info) {
        try {
            Scoreboard scoreboard = MC.theWorld.getScoreboard();
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(0);
            if (objective != null) {
                return scoreboard.getValueFromObjective(info.getGameProfile().getName(), objective).getScorePoints();
            }
        } catch (RuntimeException ignored) {}

        EntityPlayer player = FeatureUtil.findPlayer(info.getGameProfile().getId());
        return player == null ? 0 : Math.max(0, Math.round(player.getHealth()));
    }

    private boolean shouldCount(PotionState state, int gain, PotionData potion, int tick) {
        if (state.usedPots >= potion.count || tick - state.lastDetectTick < DETECT_COOLDOWN_TICKS) {
            return false;
        }
        int delta = Math.abs(gain - potion.health);
        if (delta <= 1) {
            return true;
        }
        if (delta <= 2) {
            if (state.pendingTick >= 0 && tick - state.pendingTick <= SOFT_CONFIRM_WINDOW_TICKS) {
                state.pendingTick = -1;
                return true;
            }
            state.pendingTick = tick;
        } else {
            state.pendingTick = -1;
        }
        return false;
    }

    private void recordUse(NetworkPlayerInfo info, PotionState state, PotionData potion, int tick) {
        state.usedPots++;
        state.lastDetectTick = tick;
        state.pendingTick = -1;
        FeatureUtil.refreshName(info.getGameProfile().getId());

        int hearts = potion.health / 2;
        int remaining = potion.count - state.usedPots;
        EnumChatFormatting amountColor = hearts >= 10
                ? EnumChatFormatting.LIGHT_PURPLE : EnumChatFormatting.AQUA;
        FeatureUtil.sendMessage("Potion Alert", EnumChatFormatting.LIGHT_PURPLE,
                FeatureUtil.formattedName(info) + EnumChatFormatting.YELLOW + " drank a "
                        + amountColor + hearts + " \u2764" + EnumChatFormatting.YELLOW + " potion. "
                        + EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + remaining
                        + EnumChatFormatting.GRAY + " left)");
    }

    private static void cacheName(String name, UUID uuid) {
        if (name != null && uuid != null) {
            UUIDS_BY_NAME.put(name.toLowerCase(Locale.ROOT), uuid);
        }
    }

    private static void reset() {
        Set<UUID> affectedPlayers = new HashSet<>(STATES.keySet());
        STATES.clear();
        UUIDS_BY_NAME.clear();
        for (UUID uuid : affectedPlayers) {
            FeatureUtil.refreshName(uuid);
        }
    }

    private static Map<MWClass, PotionData> createPotionData() {
        Map<MWClass, PotionData> data = new EnumMap<>(MWClass.class);
        put(data, MWClass.ANGEL, 2, 16);
        put(data, MWClass.ARCANIST, 2, 16);
        put(data, MWClass.AUTOMATON, 3, 12);
        put(data, MWClass.BLAZE, 2, 16);
        put(data, MWClass.COW, 1, 20);
        put(data, MWClass.CREEPER, 2, 16);
        put(data, MWClass.DRAGON, 2, 16);
        put(data, MWClass.DREADLORD, 2, 16);
        put(data, MWClass.ENDERMAN, 2, 16);
        put(data, MWClass.GOLEM, 2, 16);
        put(data, MWClass.HEROBRINE, 2, 12);
        put(data, MWClass.MOLEMAN, 2, 16);
        put(data, MWClass.PHOENIX, 2, 16);
        put(data, MWClass.PIGMAN, 1, 20);
        put(data, MWClass.RENEGADE, 2, 16);
        put(data, MWClass.SHAMAN, 2, 16);
        put(data, MWClass.SHARK, 2, 16);
        put(data, MWClass.SHEEP, 2, 16);
        put(data, MWClass.SKELETON, 2, 16);
        put(data, MWClass.SNOWMAN, 2, 16);
        put(data, MWClass.SPIDER, 2, 16);
        put(data, MWClass.SQUID, 3, 12);
        put(data, MWClass.WEREWOLF, 1, 20);
        put(data, MWClass.ZOMBIE, 1, 16);
        return data;
    }

    private static void put(Map<MWClass, PotionData> data, MWClass mwClass, int count, int health) {
        data.put(mwClass, new PotionData(count, health));
    }

    private static final class PotionData {
        private final int count;
        private final int health;

        private PotionData(int count, int health) {
            this.count = count;
            this.health = health;
        }
    }

    private static final class PotionState {
        private int lastHealth = -1;
        private int usedPots;
        private int lastDetectTick = Integer.MIN_VALUE / 2;
        private int pendingTick = -1;
    }
}
