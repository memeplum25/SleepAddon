package me.standonts.hud;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import fr.alexdoru.mwe.api.events.KillCounterEvent;
import fr.alexdoru.mwe.api.events.MegaWallsGameEvent;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClosestStrengthHUD extends AddonHud {

    private static final int LINE_HEIGHT = 10;
    private static final int WIDTH = 170;
    private static final int MAX_ROWS = 6;
    private final Map<String, Long> activeStrength = new HashMap<>();

    public ClosestStrengthHUD() {
        super(ExampleConfig.closestStrengthPosition);
    }

    @SubscribeEvent
    public void onKill(KillCounterEvent event) {
        if (!(event instanceof KillCounterEvent.KillEvent)) {
            return;
        }
        KillCounterEvent.KillEvent kill = (KillCounterEvent.KillEvent) event;
        MWClass killerClass = kill.killerClass == null ? MWClass.ofPlayer(kill.killer) : kill.killerClass;
        int durationTicks;
        if (killerClass == MWClass.DREADLORD) {
            durationTicks = 100;
        } else if (killerClass == MWClass.HEROBRINE) {
            durationTicks = 120;
        } else {
            return;
        }
        if (MC.theWorld != null && MC.theWorld.getPlayerEntityByName(kill.killer) != null) {
            activeStrength.put(kill.killer, System.currentTimeMillis() + durationTicks * 50L);
        }
    }

    @SubscribeEvent
    public void onGameEvent(MegaWallsGameEvent event) {
        if (event.type == MegaWallsGameEvent.Type.CONNECT
                || event.type == MegaWallsGameEvent.Type.GAME_START
                || event.type == MegaWallsGameEvent.Type.GAME_END
                || event.type == MegaWallsGameEvent.Type.DISCONNECT) {
            activeStrength.clear();
        }
    }

    @Override
    public void render(ScaledResolution resolution) {
        List<StrengthEntry> entries = getVisibleEntries();
        if (entries.isEmpty()) {
            return;
        }
        int height = entries.size() * LINE_HEIGHT;
        getPosition().updateAdjustedAbsolutePosition(resolution, WIDTH, height);
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        //drawRect(x - 2, y - 2, x + WIDTH, y + height + 1, 0x90000000);

        long now = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); i++) {
            StrengthEntry entry = entries.get(i);
            float seconds = Math.max(0L, entry.expiresAt - now) / 1000.0F;
            EnumChatFormatting timeColor = seconds <= 1.0F ? EnumChatFormatting.RED
                    : seconds <= 3.0F ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN;
            int distance = (int) MC.thePlayer.getDistanceToEntity(entry.player);
            String name = entry.player.getDisplayName().getFormattedText();
            String text = name + " " + timeColor + String.format("%.1fs", seconds)
                    + EnumChatFormatting.GRAY + " " + distance + "m";
            int rowY = y + i * LINE_HEIGHT;
            drawPlayerHead(entry.player, x, rowY + 1);
            MC.fontRendererObj.drawStringWithShadow(text, x + 10, rowY + 1, 0xFFFFFF);
        }
    }

    @Override
    public void renderDummy() {
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        //drawRect(x - 2, y - 2, x + WIDTH, y + LINE_HEIGHT + 1, 0x90000000);
        drawPlayerHead(MC.thePlayer, x, y + 1);
        MC.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Player "
                + EnumChatFormatting.GREEN + "5.0s" + EnumChatFormatting.GRAY + " 10m",
                x + 10, y + 1, 0xFFFFFF);
    }

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        removeExpired(currentTimeMillis);
        return getPosition().isEnabled() && MC.thePlayer != null && MC.theWorld != null
                && MWEApi.Scoreboard.getScoreboardParser().isInMwGame()
                && !activeStrength.isEmpty();
    }

    private List<StrengthEntry> getVisibleEntries() {
        long now = System.currentTimeMillis();
        removeExpired(now);
        List<StrengthEntry> entries = new ArrayList<>();
        if (MC.theWorld == null || MC.thePlayer == null) {
            return entries;
        }
        for (Map.Entry<String, Long> entry : activeStrength.entrySet()) {
            EntityPlayer player = MC.theWorld.getPlayerEntityByName(entry.getKey());
            if (player != null) {
                entries.add(new StrengthEntry(player, entry.getValue()));
            }
        }
        entries.sort(Comparator.comparingDouble(value -> MC.thePlayer.getDistanceSqToEntity(value.player)));
        if (entries.size() > MAX_ROWS) {
            return new ArrayList<>(entries.subList(0, MAX_ROWS));
        }
        return entries;
    }

    private void removeExpired(long now) {
        activeStrength.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static final class StrengthEntry {
        private final EntityPlayer player;
        private final long expiresAt;

        private StrengthEntry(EntityPlayer player, long expiresAt) {
            this.player = player;
            this.expiresAt = expiresAt;
        }
    }
}
