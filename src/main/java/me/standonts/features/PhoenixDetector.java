package me.standonts.features;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import fr.alexdoru.mwe.api.enums.MWTeam;
import fr.alexdoru.mwe.api.events.MegaWallsGameEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PhoenixDetector {

    private static final float UNKNOWN_HEALTH = -1.0F;
    private static final float DEAD_HEALTH = 0.1F;
    private static final float LOW_HEALTH = 12.0F;
    private static final float RESURRECTION_HEALTH = 40.0F;
    private static final long JOIN_GRACE_MS = 1500L;
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<UUID, PhoenixState> STATES = new HashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ExampleConfig.phoenixDetector
                || MC.theWorld == null || MC.thePlayer == null || MC.getNetHandler() == null
                || !FeatureUtil.isInMegaWallsGame()
                || MWEApi.Scoreboard.getScoreboardParser().getWitherCount() >= 4) {
            return;
        }

        Scoreboard scoreboard = MC.theWorld.getScoreboard();
        ScoreObjective healthObjective = scoreboard.getObjectiveInDisplaySlot(0);
        long now = System.currentTimeMillis();
        for (NetworkPlayerInfo info : MC.getNetHandler().getPlayerInfoMap()) {
            if (!isEligiblePhoenix(info) || isOwnWitherAlive(info)) {
                continue;
            }

            UUID uuid = info.getGameProfile().getId();
            PhoenixState state = STATES.computeIfAbsent(uuid, ignored -> new PhoenixState(0L));
            if (state.used) {
                continue;
            }
            EntityPlayer entity = FeatureUtil.findPlayer(uuid);
            float health = resolveHealth(info, entity, scoreboard, healthObjective, state, now);
            if (health < 0.0F) {
                continue;
            }

            boolean riding = entity != null && entity.isRiding();
            if (state.health < 0.0F) {
                state.health = health;
                state.riding = riding;
                continue;
            }

            boolean resurrected = state.dead && health >= RESURRECTION_HEALTH;
            String reason = resurrected ? "dead-to-full" : null;
            if (!resurrected && state.health <= LOW_HEALTH && health >= RESURRECTION_HEALTH
                    && (state.joinedAt == 0L || now - state.joinedAt > JOIN_GRACE_MS)) {
                resurrected = true;
                reason = "low-to-full";
            }
            if (!resurrected && riding && !state.riding) {
                resurrected = true;
                reason = "ride-trigger";
            }

            state.health = health;
            state.riding = riding;
            state.dead = health < DEAD_HEALTH;
            if (resurrected) {
                state.used = true;
                state.dead = false;
                notifyResurrection(info, reason);
                FeatureUtil.refreshName(uuid);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinWorldEvent event) {
        if (!ExampleConfig.phoenixDetector || !(event.entity instanceof EntityPlayer)
                || !event.world.isRemote || !FeatureUtil.isInMegaWallsGame()
                || MWEApi.Scoreboard.getScoreboardParser().getWitherCount() >= 4) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entity;
        if (MWEApi.Player.getPlayerInfo(player).getMWClass() == MWClass.PHOENIX) {
            PhoenixState previous = STATES.get(player.getUniqueID());
            if (previous == null) {
                STATES.put(player.getUniqueID(), new PhoenixState(System.currentTimeMillis()));
            } else if (!previous.used) {
                previous.joinedAt = System.currentTimeMillis();
                previous.riding = false;
            }
        }
    }

    @SubscribeEvent
    public void onGameEvent(MegaWallsGameEvent event) {
        if (event.type == MegaWallsGameEvent.Type.CONNECT
                || event.type == MegaWallsGameEvent.Type.GAME_START
                || event.type == MegaWallsGameEvent.Type.GAME_END
                || event.type == MegaWallsGameEvent.Type.DISCONNECT) {
            reset();
        }
    }

    public static String getResurrectionIcon(String playerName) {
        if (playerName == null || MC.getNetHandler() == null) {
            return null;
        }
        NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(playerName);
        PhoenixState state = info == null || info.getGameProfile() == null
                ? null : STATES.get(info.getGameProfile().getId());
        if (state == null) {
            return null;
        }
        return (state.used ? EnumChatFormatting.GRAY : EnumChatFormatting.LIGHT_PURPLE) + "\u03A9";
    }

    private boolean isEligiblePhoenix(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null
                || info.getGameType() == GameType.SPECTATOR) {
            return false;
        }
        EntityPlayer entity = FeatureUtil.findPlayer(info.getGameProfile().getId());
        if (entity != null && MWEApi.Player.getPlayerInfo(entity).getMWClass() == MWClass.PHOENIX) {
            return true;
        }
        return MWClass.ofPlayer(info.getGameProfile().getId()) == MWClass.PHOENIX;
    }

    private boolean isOwnWitherAlive(NetworkPlayerInfo info) {
        MWTeam team = MWTeam.ofPlayer(info.getGameProfile().getId());
        return team == null || MWEApi.Scoreboard.getScoreboardParser().getAliveWithers().contains(team);
    }

    private float resolveHealth(NetworkPlayerInfo info, EntityPlayer entity, Scoreboard scoreboard,
                                ScoreObjective objective, PhoenixState state, long now) {
        try {
            if (objective != null) {
                return scoreboard.getValueFromObjective(info.getGameProfile().getName(), objective).getScorePoints();
            }
        } catch (RuntimeException ignored) {}
        if (entity == null || now - state.joinedAt <= JOIN_GRACE_MS) {
            return UNKNOWN_HEALTH;
        }
        return entity.getHealth();
    }

    private void notifyResurrection(NetworkPlayerInfo info, String reason) {
        FeatureUtil.sendMessage("Phoenix Alert", EnumChatFormatting.GOLD,
                FeatureUtil.formattedName(info) + EnumChatFormatting.YELLOW + " resurrected!"
                        + (ExampleConfig.denickDebugLogging
                        ? EnumChatFormatting.DARK_GRAY + " (" + reason + ")" : ""));
        if (MC.thePlayer != null) {
            MC.thePlayer.playSound("note.pling", 1.0F, 2.0F);
        }
    }

    private static void reset() {
        Set<UUID> affectedPlayers = new HashSet<>(STATES.keySet());
        STATES.clear();
        for (UUID uuid : affectedPlayers) {
            FeatureUtil.refreshName(uuid);
        }
    }

    private static final class PhoenixState {
        private float health = UNKNOWN_HEALTH;
        private long joinedAt;
        private boolean dead;
        private boolean riding;
        private boolean used;

        private PhoenixState(long joinedAt) {
            this.joinedAt = joinedAt;
        }
    }
}
