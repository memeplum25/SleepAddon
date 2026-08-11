package me.standonts.hud;

import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import fr.alexdoru.mwe.api.events.MegaWallsGameEvent;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class LeapModeHUD extends AddonHud {

    private static final String ARROW_MESSAGE =
            "Your primary Leap skill switched to Arrow mode.";
    private static final String ARCED_MESSAGE =
            "Your primary Leap skill switched to Arced mode.";

    private LeapMode currentMode = LeapMode.ARROW;

    public LeapModeHUD() {
        super(ExampleConfig.leapModePosition);
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (event.message == null) {
            return;
        }
        String message = event.message.getUnformattedText();
        if (ARROW_MESSAGE.equals(message)) {
            currentMode = LeapMode.ARROW;
        } else if (ARCED_MESSAGE.equals(message)) {
            currentMode = LeapMode.ARCED;
        }
    }

    @SubscribeEvent
    public void onGameEvent(MegaWallsGameEvent event) {
        if (event.type == MegaWallsGameEvent.Type.CONNECT
                || event.type == MegaWallsGameEvent.Type.GAME_START
                || event.type == MegaWallsGameEvent.Type.GAME_END
                || event.type == MegaWallsGameEvent.Type.DISCONNECT) {
            currentMode = LeapMode.ARROW;
        }
    }

    @Override
    public void render(ScaledResolution resolution) {
        drawMode(resolution, currentMode);
    }

    @Override
    public void renderDummy() {
        drawMode(null, LeapMode.ARROW);
    }

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        if (!getPosition().isEnabled() || MC.thePlayer == null
                || !MWEApi.Scoreboard.getScoreboardParser().isInMwGame()) {
            return false;
        }
        MWClass playerClass = MWEApi.Player.getPlayerInfo(MC.thePlayer).getMWClass();
        if (playerClass == null) {
            playerClass = MWClass.ofPlayer(MC.thePlayer.getUniqueID());
        }
        return playerClass == MWClass.SPIDER;
    }

    private void drawMode(ScaledResolution resolution, LeapMode mode) {
        String text = EnumChatFormatting.AQUA + mode.displayName;
        int width = MC.fontRendererObj.getStringWidth(text);
        if (resolution != null) {
            getPosition().updateAdjustedAbsolutePosition(
                    resolution, width, MC.fontRendererObj.FONT_HEIGHT);
        }
        MC.fontRendererObj.drawStringWithShadow(text,
                getPosition().getAbsoluteRenderX(),
                getPosition().getAbsoluteRenderY(), 0xFFFFFF);
    }

    private enum LeapMode {
        ARROW("Arrow"),
        ARCED("Arced");

        private final String displayName;

        LeapMode(String displayName) {
            this.displayName = displayName;
        }
    }
}
