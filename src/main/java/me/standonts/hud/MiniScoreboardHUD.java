package me.standonts.hud;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import fr.alexdoru.mwe.api.enums.MWTeam;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MiniScoreboardHUD extends AddonHud {

    private static final Pattern GATES_OPEN = Pattern.compile("Gates Open:\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern WALLS_FALL = Pattern.compile("Walls Fall:\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern ENRAGE_OFF = Pattern.compile("Enrage Off:\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern GAME_END = Pattern.compile("Game End:\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern WITHER_HP = Pattern.compile("\\[([BGRY])\\]\\s*Wither HP:\\s*([\\d,]+)");
    private static final Pattern TEAM_PLAYERS = Pattern.compile(
            "\\[([BGRY])\\]\\s*(?:Players?\\s*:\\s*([\\d,]+)|([\\d,]+)\\s*Players?)");
    private static final Pattern FINALS_ASSISTS = Pattern.compile(
            "(\\d+)\\s*F\\.\\s*Kills?\\s*(\\d+)\\s*F\\.\\s*Assists?");
    private static final Pattern PREGAME_PLAYERS = Pattern.compile("Players:\\s*([0-9]+/[0-9]+)");
    private static final Pattern PREGAME_START = Pattern.compile("Starting in\\s*([0-9]{1,2}:[0-9]{2})");
    private static final Pattern PREGAME_MAP = Pattern.compile("Map:\\s*([A-Za-z0-9_ ]+)");
    private static final Pattern PREGAME_CLASS = Pattern.compile("Selected Class:\\s*([A-Za-z]+)?");
    private static final String SEPARATOR = EnumChatFormatting.DARK_GRAY + " | "
            + EnumChatFormatting.RESET;
    private static final String LABEL = EnumChatFormatting.GRAY.toString();
    private static final String VALUE = EnumChatFormatting.WHITE.toString();

    private final WitherEtaEstimator etaEstimator = new WitherEtaEstimator();
    private String displayText = "";

    public MiniScoreboardHUD() {
        super(ExampleConfig.miniScoreboardPosition);
    }

    @Override
    public void render(ScaledResolution resolution) {
        displayText = buildDisplayText();
        if (displayText.isEmpty()) {
            return;
        }
        List<String> displayLines = wrapText(displayText, Math.max(120, resolution.getScaledWidth() - 8));
        int width = 0;
        for (String line : displayLines) {
            width = Math.max(width, MC.fontRendererObj.getStringWidth(line));
        }
        int lineHeight = MC.fontRendererObj.FONT_HEIGHT + 1;
        int height = displayLines.size() * lineHeight;
        getPosition().updateAdjustedAbsolutePosition(resolution, width, height);
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        if (ExampleConfig.miniScoreboardBackground) {
            drawRect(x - 3, y - 2, x + width + 3, y + height + 1, 0x90000000);
        }
        for (int i = 0; i < displayLines.size(); i++) {
            MC.fontRendererObj.drawStringWithShadow(displayLines.get(i), x, y + i * lineHeight, 0xFFFFFF);
        }
    }

    @Override
    public void renderDummy() {
        String text = EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD + "Walls Fall"
                + EnumChatFormatting.RESET + VALUE + ":00:04" + SEPARATOR
                + EnumChatFormatting.BLUE + "B" + VALUE + ":1000" + LABEL + "HP(2) "
                + EnumChatFormatting.GREEN + "G" + VALUE + ":1000" + LABEL + "HP(3) "
                + EnumChatFormatting.RED + "R" + VALUE + ":1000" + LABEL + "HP(2) "
                + EnumChatFormatting.GOLD + "Y" + VALUE + ":1000" + LABEL + "HP(0)"
                + SEPARATOR + LABEL + "FKs" + VALUE + ":10 " + LABEL + "FKAs" + VALUE + ":8";
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        int width = MC.fontRendererObj.getStringWidth(text);
        if (ExampleConfig.miniScoreboardBackground) {
            drawRect(x - 3, y - 2, x + width + 3,
                    y + MC.fontRendererObj.FONT_HEIGHT + 1, 0x90000000);
        }
        MC.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFF);
    }

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        return getPosition().isEnabled()
                && (MWEApi.Scoreboard.getScoreboardParser().isInMwGame()
                || MWEApi.Scoreboard.getScoreboardParser().isPreGameLobby());
    }

    private String buildDisplayText() {
        List<String> lines = MWEApi.Scoreboard.getUnformattedSidebarText();
        if (lines.isEmpty()) {
            etaEstimator.reset();
            return "";
        }
        if (MWEApi.Scoreboard.getScoreboardParser().isPreGameLobby()) {
            etaEstimator.reset();
            return buildPregameText(lines);
        }

        String phaseLabel = "Walls Fall";
        String phaseTime = "--:--";
        String finals = "0";
        String assists = "0";
        Map<MWTeam, TeamValue> teams = new EnumMap<>(MWTeam.class);
        List<MWTeam> order = new ArrayList<>();

        for (String line : lines) {
            String value = matchGroup(GATES_OPEN, line);
            if (value != null) {
                phaseLabel = "Gates Open";
                phaseTime = value;
                continue;
            }
            value = matchGroup(WALLS_FALL, line);
            if (value != null) {
                phaseLabel = "Walls Fall";
                phaseTime = value;
                continue;
            }
            value = matchGroup(ENRAGE_OFF, line);
            if (value != null) {
                phaseLabel = "Enrage Off";
                phaseTime = value;
                continue;
            }
            value = matchGroup(GAME_END, line);
            if (value != null) {
                phaseLabel = "Game End";
                phaseTime = value;
                continue;
            }

            Matcher wither = WITHER_HP.matcher(line);
            if (wither.find()) {
                MWTeam team = teamFromLetter(wither.group(1).charAt(0));
                if (team != null) {
                    addTeam(order, team);
                    teams.put(team, new TeamValue(Integer.parseInt(wither.group(2).replace(",", "")), true));
                }
                continue;
            }
            Matcher players = TEAM_PLAYERS.matcher(line);
            if (players.find()) {
                MWTeam team = teamFromLetter(players.group(1).charAt(0));
                String count = players.group(2) == null ? players.group(3) : players.group(2);
                if (team != null) {
                    addTeam(order, team);
                    teams.put(team, new TeamValue(Integer.parseInt(count.replace(",", "")), false));
                }
                continue;
            }
            Matcher score = FINALS_ASSISTS.matcher(line);
            if (score.find()) {
                finals = score.group(1);
                assists = score.group(2);
            }
        }

        if (order.isEmpty()) {
            order.add(MWTeam.BLUE);
            order.add(MWTeam.GREEN);
            order.add(MWTeam.RED);
            order.add(MWTeam.YELLOW);
        }
        String eta = updateWitherEta(teams);
        StringBuilder teamText = new StringBuilder();
        for (MWTeam team : order) {
            if (teamText.length() > 0) {
                teamText.append(' ');
            }
            TeamValue teamValue = teams.get(team);
            teamText.append(team.getColorPrefix()).append(teamLetter(team)).append(VALUE).append(':')
                    .append(teamValue == null ? "-" : teamValue.value);
            if (teamValue != null && teamValue.witherHealth) {
                teamText.append(LABEL).append("HP");
            }
            if (ExampleConfig.miniScoreboardTeamFinals) {
                teamText.append(EnumChatFormatting.GRAY).append('(').append(team.getColorPrefix())
                        .append(MWEApi.FinalKills.getKillsOfTeam(team))
                        .append(EnumChatFormatting.GRAY).append(')');
            }
            if (teamValue != null && teamValue.witherHealth
                    && MWEApi.Scoreboard.getScoreboardParser().getAliveWithers().contains(team)
                    && !eta.isEmpty()) {
                teamText.append(EnumChatFormatting.GRAY).append(eta);
            }
        }

        return EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD + phaseLabel
                + EnumChatFormatting.RESET + VALUE + ':' + phaseTime + SEPARATOR + teamText
                + SEPARATOR + LABEL + "FKs" + VALUE + ':' + finals + ' '
                + LABEL + "FKAs" + VALUE + ':' + assists;
    }

    private String buildPregameText(List<String> lines) {
        String selectedClass = "-";
        String players = "-/-";
        String start = "--:--";
        String map = "-";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher classMatcher = PREGAME_CLASS.matcher(line);
            if (classMatcher.find()) {
                if (classMatcher.group(1) != null && !classMatcher.group(1).isEmpty()) {
                    selectedClass = classMatcher.group(1);
                } else if (i + 1 < lines.size()) {
                    String found = findKnownClass(lines.get(i + 1));
                    selectedClass = found == null ? selectedClass : found;
                }
            } else if ("-".equals(selectedClass)) {
                String found = findKnownClass(line);
                selectedClass = found == null ? selectedClass : found;
            }

            String value = matchGroup(PREGAME_PLAYERS, line);
            if (value != null) {
                players = value;
            }
            value = matchGroup(PREGAME_START, line);
            if (value != null) {
                start = value;
            }
            value = matchGroup(PREGAME_MAP, line);
            if (value != null) {
                map = value.trim();
            }
        }
        return EnumChatFormatting.YELLOW.toString() + EnumChatFormatting.BOLD + "PRE"
                + EnumChatFormatting.RESET + VALUE + " MW" + SEPARATOR + LABEL + "Class"
                + VALUE + ':' + selectedClass + SEPARATOR + LABEL + "Players" + VALUE + ':'
                + players + SEPARATOR + LABEL + "Start" + VALUE + ':' + start
                + SEPARATOR + LABEL + "Map" + VALUE + ':' + map;
    }

    private String updateWitherEta(Map<MWTeam, TeamValue> teams) {
        List<MWTeam> alive = MWEApi.Scoreboard.getScoreboardParser().getAliveWithers();
        if (alive.size() != 1) {
            etaEstimator.reset();
            return "";
        }
        MWTeam team = alive.get(0);
        TeamValue value = teams.get(team);
        if (value == null || !value.witherHealth) {
            etaEstimator.reset();
            return "";
        }
        int seconds = etaEstimator.update(team, value.value, System.currentTimeMillis());
        return seconds < 0 ? "" : String.format(Locale.ROOT, "(%02d:%02d)", seconds / 60, seconds % 60);
    }

    private String findKnownClass(String line) {
        String normalized = line == null ? "" : line.toLowerCase(Locale.ROOT);
        for (MWClass mwClass : MWClass.values()) {
            if (normalized.contains(mwClass.className.toLowerCase(Locale.ROOT))) {
                return mwClass.className;
            }
        }
        return null;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        String[] parts = text.split(Pattern.quote(SEPARATOR));
        String current = "";
        for (String part : parts) {
            String candidate = current.isEmpty() ? part : current + SEPARATOR + part;
            if (!current.isEmpty() && MC.fontRendererObj.getStringWidth(candidate) > maxWidth) {
                wrapped.add(current);
                current = part;
            } else {
                current = candidate;
            }
        }
        if (!current.isEmpty()) {
            wrapped.add(current);
        }
        return wrapped;
    }

    private String matchGroup(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void addTeam(List<MWTeam> order, MWTeam team) {
        if (!order.contains(team)) {
            order.add(team);
        }
    }

    private MWTeam teamFromLetter(char letter) {
        switch (letter) {
            case 'B': return MWTeam.BLUE;
            case 'G': return MWTeam.GREEN;
            case 'R': return MWTeam.RED;
            case 'Y': return MWTeam.YELLOW;
            default: return null;
        }
    }

    private char teamLetter(MWTeam team) {
        switch (team) {
            case BLUE: return 'B';
            case GREEN: return 'G';
            case RED: return 'R';
            case YELLOW: return 'Y';
            default: return '?';
        }
    }

    private static final class TeamValue {
        private final int value;
        private final boolean witherHealth;

        private TeamValue(int value, boolean witherHealth) {
            this.value = value;
            this.witherHealth = witherHealth;
        }
    }

    private static final class WitherEtaEstimator {
        private MWTeam team;
        private int previousHealth = -1;
        private long previousUpdate;
        private double damagePerSecond;

        private int update(MWTeam currentTeam, int health, long now) {
            if (team != currentTeam) {
                reset();
                team = currentTeam;
            }
            if (previousHealth < 0) {
                previousHealth = health;
                previousUpdate = now;
                return -1;
            }
            if (health != previousHealth) {
                long elapsed = now - previousUpdate;
                if (health < previousHealth && elapsed >= 100L && elapsed <= 10_000L) {
                    double instantRate = (previousHealth - health) * 1000.0D / elapsed;
                    damagePerSecond = damagePerSecond == 0.0D
                            ? instantRate : damagePerSecond * 0.7D + instantRate * 0.3D;
                } else if (health > previousHealth) {
                    damagePerSecond = 0.0D;
                }
                previousHealth = health;
                previousUpdate = now;
            }
            if (damagePerSecond < 0.05D) {
                return -1;
            }
            if (now - previousUpdate > 5_000L) {
                return -1;
            }
            return Math.min(5999, (int) Math.ceil(health / damagePerSecond));
        }

        private void reset() {
            team = null;
            previousHealth = -1;
            previousUpdate = 0L;
            damagePerSecond = 0.0D;
        }
    }
}
