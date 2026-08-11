package me.standonts.asm.hooks;

import fr.alexdoru.configlib.api.IRenderer;
import fr.alexdoru.configlib.api.RendererPosition;
import fr.alexdoru.mwe.api.enums.MWMap;
import fr.alexdoru.mwe.api.enums.MWTeam;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

public final class BaseLocationHudHook {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final String DUMMY_TEXT = EnumChatFormatting.GREEN + "GREEN MAIN CENTER"
            + EnumChatFormatting.GRAY + " (" + EnumChatFormatting.RED + "RED"
            + EnumChatFormatting.GRAY + ")";

    private BaseLocationHudHook() {}

    public static void render(IRenderer hud, MWMap map, ScaledResolution resolution) {
        if (map == null || MC.thePlayer == null || MC.fontRendererObj == null) {
            return;
        }

        String text = computeLocationText(map, MC.thePlayer.posX, MC.thePlayer.posZ);
        if (text.isEmpty()) {
            return;
        }

        int width = MC.fontRendererObj.getStringWidth(text);
        int height = MC.fontRendererObj.FONT_HEIGHT;
        RendererPosition position = hud.getPosition();
        position.updateAdjustedAbsolutePosition(resolution, width, height);
        drawCentered(position, text, width, height);
    }

    public static void renderDummy(IRenderer hud) {
        if (MC.fontRendererObj == null) {
            return;
        }
        int width = MC.fontRendererObj.getStringWidth(DUMMY_TEXT);
        drawCentered(hud.getPosition(), DUMMY_TEXT, width, MC.fontRendererObj.FONT_HEIGHT);
    }

    private static void drawCentered(RendererPosition position, String text, int width, int height) {
        int centerX = position.getAbsoluteRenderX();
        int y = position.getAbsoluteRenderY();
        int left = centerX - width / 2;

        MC.fontRendererObj.drawStringWithShadow(text, left, y, 0xFFFFFF);
    }

    private static String computeLocationText(MWMap map, double x, double z) {
        String baseGrid = getBaseNineSlice(map, x, z);
        if (!baseGrid.isEmpty()) {
            return baseGrid;
        }
        if (!isAtMiddle(map, x, z)) {
            return "";
        }

        int side = getSideIndexAt(map, x, z);
        MWTeam nearestBase = getSideTeams(map)[side];
        if (isAtMidMid(map, x, z)) {
            return EnumChatFormatting.AQUA + "MID" + EnumChatFormatting.GRAY
                    + " (" + nearestBase.formattedName() + EnumChatFormatting.GRAY + ")";
        }

        MWTeam neighbor = getCloserNeighbor(map, x, z, side);
        return nearestBase.formattedName() + EnumChatFormatting.GOLD + " FRONT"
                + EnumChatFormatting.GRAY + " (" + neighbor.formattedName()
                + EnumChatFormatting.GRAY + ")";
    }

    private static MWTeam getCloserNeighbor(MWMap map, double x, double z, int side) {
        double width = map.eastLimit - map.westLimit;
        double height = map.southLimit - map.northLimit;
        double position;
        switch (side) {
            case 0:
                position = (map.eastLimit - x) / width;
                break;
            case 1:
                position = (map.southLimit - z) / height;
                break;
            case 2:
                position = (x - map.westLimit) / width;
                break;
            case 3:
                position = (z - map.northLimit) / height;
                break;
            default:
                position = 0.5D;
        }
        MWTeam[] teams = getSideTeams(map);
        return position <= 0.5D ? teams[(side + 1) % 4] : teams[(side + 3) % 4];
    }

    private static int getSideIndexAt(MWMap map, double x, double z) {
        boolean firstDiagonal = x > -z + map.eastLimit + map.northLimit;
        boolean secondDiagonal = x > z + map.westLimit - map.northLimit;
        return firstDiagonal && secondDiagonal ? 1
                : !firstDiagonal && secondDiagonal ? 0
                : !firstDiagonal ? 3 : 2;
    }

    private static MWTeam[] getSideTeams(MWMap map) {
        return new MWTeam[]{map.northBase, map.eastBase, map.southBase, map.westBase};
    }

    private static String getBaseNineSlice(MWMap map, double x, double z) {
        int side = -1;
        double width = map.eastLimit - map.westLimit;
        double height = map.southLimit - map.northLimit;
        double depth = -1.0D;

        if (z < map.northLimit && x > map.westLimit && x < map.eastLimit) {
            side = 0;
            depth = (map.northLimit - z) / height;
        } else if (x > map.eastLimit && z > map.northLimit && z < map.southLimit) {
            side = 1;
            depth = (x - map.eastLimit) / width;
        } else if (z > map.southLimit && x > map.westLimit && x < map.eastLimit) {
            side = 2;
            depth = (z - map.southLimit) / height;
        } else if (x < map.westLimit && z > map.northLimit && z < map.southLimit) {
            side = 3;
            depth = (map.westLimit - x) / width;
        }

        if (side == -1 || depth < 0.0D || depth > 1.2D) {
            return "";
        }

        String row = depth < 0.33D ? "PRE" : depth < 0.66D ? "MAIN" : "REAR";
        double lateral;
        switch (side) {
            case 0:
                lateral = (map.eastLimit - x) / width;
                break;
            case 1:
                lateral = (map.southLimit - z) / height;
                break;
            case 2:
                lateral = (x - map.westLimit) / width;
                break;
            default:
                lateral = (z - map.northLimit) / height;
        }

        String column = lateral < 0.33D ? "SPAWN-SIDE"
                : lateral < 0.66D ? "CENTER" : "FAR-SIDE";
        MWTeam neighbor = getCloserNeighbor(map, x, z, side);
        return getSideTeams(map)[side].formattedName() + " " + row + " " + column
                + EnumChatFormatting.GRAY + " (" + neighbor.formattedName()
                + EnumChatFormatting.GRAY + ")";
    }

    private static boolean isAtMiddle(MWMap map, double x, double z) {
        return x < map.eastLimit && x > map.westLimit
                && z < map.southLimit && z > map.northLimit;
    }

    private static boolean isAtMidMid(MWMap map, double x, double z) {
        return x < map.innerEastLimit && x > map.innerWestLimit
                && z < map.innerSouthLimit && z > map.innerNorthLimit;
    }
}
