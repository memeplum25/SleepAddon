package me.standonts.features;

import me.standonts.config.ExampleConfig;
import com.mojang.authlib.GameProfile;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.MWECommandBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Denick {

    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final long FAILED_LOOKUP_RETRY_MS = 10_000L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<String, Resolution> GROUP_RESOLUTIONS = new ConcurrentHashMap<>();
    private static final Map<String, Resolution> NICK_RESOLUTIONS = new ConcurrentHashMap<>();

    private final Map<UUID, String> resolvedNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> failedLookups = new ConcurrentHashMap<>();
    private final Set<UUID> pendingLookups = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<UUID>> previousGroupUuids = new HashMap<>();
    private final Map<String, AliasRecord> sessionAliases = new HashMap<>();
    private final Set<String> announcedResolutions = new HashSet<>();
    private int refreshCooldown;
    private boolean wasEnabled = true;

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        clearState(true);
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clearState(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ExampleConfig.denick) {
            if (wasEnabled) {
                clearState(true);
                wasEnabled = false;
            }
            return;
        }
        wasEnabled = true;
        if (refreshCooldown-- <= 0) {
            refreshCooldown = REFRESH_INTERVAL_TICKS;
            refreshPlayerListState();
        }
    }

    public static String resolveRealName(String name) {
        Resolution resolution = findResolution(name);
        return resolution == null ? name : resolution.realName;
    }

    public static UUID resolveRealUuid(String name) {
        Resolution resolution = findResolution(name);
        return resolution == null ? null : resolution.realUuid;
    }

    public static boolean isNickedPlayer(UUID uuid) {
        return uuid != null && uuid.version() == 1;
    }

    private void refreshPlayerListState() {
        if (MC.theWorld == null || MC.thePlayer == null || MC.isSingleplayer() || MC.getNetHandler() == null) {
            previousGroupUuids.clear();
            return;
        }

        Map<String, PlayerGroup> groups = groupPlayers(MC.getNetHandler().getPlayerInfoMap());
        for (Map.Entry<String, PlayerGroup> entry : groups.entrySet()) {
            String groupKey = entry.getKey();
            PlayerGroup group = entry.getValue();
            if (!group.hasNickedPlayer || group.realUuids.isEmpty() || group.players.size() < 2) {
                continue;
            }

            String detectionType = isRejoin(groupKey, group.uuids) ? "rejoin" : "duplicate";
            Resolution resolution = GROUP_RESOLUTIONS.get(groupKey);
            if (resolution == null) {
                for (UUID realUuid : group.realUuids) {
                    queueLookup(realUuid);
                    String realName = resolvedNames.get(realUuid);
                    if (realName != null && !realName.isEmpty()) {
                        resolution = new Resolution(group.displayName, realName, realUuid);
                        GROUP_RESOLUTIONS.put(groupKey, resolution);
                        break;
                    }
                }
            }

            if (resolution != null) {
                applyResolution(group, resolution);
                String announcementKey = groupKey + '|' + resolution.realUuid;
                if (announcedResolutions.add(announcementKey)) {
                    announceResolution(detectionType, resolution);
                }
            } else if (ExampleConfig.denickDebugLogging) {
                FeatureUtil.sendMessage("DeNick", EnumChatFormatting.DARK_GRAY,
                        EnumChatFormatting.GRAY + detectionType + " group " + group.displayName
                                + " is waiting for " + group.realUuids.size() + " profile lookup(s)");
            }
        }

        previousGroupUuids.clear();
        for (Map.Entry<String, PlayerGroup> entry : groups.entrySet()) {
            previousGroupUuids.put(entry.getKey(), new HashSet<>(entry.getValue().uuids));
        }
    }

    private Map<String, PlayerGroup> groupPlayers(Collection<NetworkPlayerInfo> playerInfos) {
        Map<String, PlayerGroup> groups = new HashMap<>();
        for (NetworkPlayerInfo info : playerInfos) {
            GameProfile profile = info == null ? null : info.getGameProfile();
            if (profile == null || profile.getId() == null || profile.getName() == null
                    || profile.getName().isEmpty()) {
                continue;
            }
            String displayName = getGroupingName(info);
            if (displayName == null || displayName.isEmpty()) {
                displayName = profile.getName();
            }
            final String groupingName = displayName;
            String key = normalizeName(groupingName);
            PlayerGroup group = groups.computeIfAbsent(key, ignored -> new PlayerGroup(groupingName));
            group.players.add(info);
            group.uuids.add(profile.getId());
            if (isNickedPlayer(profile.getId())) {
                group.hasNickedPlayer = true;
            } else if (profile.getId().version() == 4) {
                group.realUuids.add(profile.getId());
            }
        }
        return groups;
    }

    private boolean isRejoin(String groupKey, Set<UUID> currentUuids) {
        Set<UUID> previous = previousGroupUuids.get(groupKey);
        if (previous == null || previous.isEmpty()) {
            return false;
        }
        for (UUID uuid : currentUuids) {
            if (!previous.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    private void applyResolution(PlayerGroup group, Resolution resolution) {
        for (NetworkPlayerInfo info : group.players) {
            GameProfile profile = info.getGameProfile();
            if (!isNickedPlayer(profile.getId())) {
                continue;
            }
            String nickName = profile.getName();
            NICK_RESOLUTIONS.put(normalizeName(nickName), resolution);
            String aliasKey = normalizeName(nickName);
            if (!sessionAliases.containsKey(aliasKey)
                    && MWEApi.Alias.getAlias(profile.getId(), nickName) == null) {
                MWEApi.Alias.setAlias(profile.getId(), nickName, resolution.realName);
                sessionAliases.put(aliasKey, new AliasRecord(profile.getId(), nickName, resolution.realName));
            }
            FeatureUtil.refreshName(profile.getId());
        }
    }

    private void queueLookup(UUID uuid) {
        Long failedAt = failedLookups.get(uuid);
        if (resolvedNames.containsKey(uuid) || pendingLookups.contains(uuid)
                || failedAt != null && System.currentTimeMillis() - failedAt < FAILED_LOOKUP_RETRY_MS
                || !pendingLookups.add(uuid)) {
            return;
        }

        if (ExampleConfig.denickDebugLogging) {
            FeatureUtil.sendMessage("DeNick", EnumChatFormatting.DARK_GRAY,
                    EnumChatFormatting.GRAY + "looking up " + normalizeUuid(uuid));
        }
        MWEApi.Tasks.queueIOTask(() -> {
            String playerName = null;
            try {
                playerName = MWEApi.MojangApi.getPlayerName(uuid);
            } catch (Exception ignored) {}
            final String result = playerName;
            MC.addScheduledTask(() -> {
                pendingLookups.remove(uuid);
                if (result == null || result.isEmpty()) {
                    failedLookups.put(uuid, System.currentTimeMillis());
                } else {
                    failedLookups.remove(uuid);
                    resolvedNames.put(uuid, result);
                    refreshCooldown = 0;
                }
                if (ExampleConfig.denickDebugLogging) {
                    FeatureUtil.sendMessage("DeNick", EnumChatFormatting.DARK_GRAY,
                            EnumChatFormatting.GRAY + "lookup " + normalizeUuid(uuid) + " -> "
                                    + (result == null ? "not found" : result));
                }
            });
        });
    }

    private void announceResolution(String detectionType, Resolution resolution) {
        ChatComponentText message = new ChatComponentText(EnumChatFormatting.DARK_GRAY + "["
                + EnumChatFormatting.GOLD + "DeNick" + EnumChatFormatting.DARK_GRAY + "] "
                + EnumChatFormatting.GRAY + detectionType + " resolved " + EnumChatFormatting.RED
                + resolution.fakeName + EnumChatFormatting.GRAY + " -> " + EnumChatFormatting.AQUA
                + resolution.realName + EnumChatFormatting.GRAY + " ");
        ChatComponentText copy = new ChatComponentText(EnumChatFormatting.YELLOW + "[COPY]");
        copy.setChatStyle(new ChatStyle()
                .setUnderlined(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/denick copy " + resolution.fakeName))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(EnumChatFormatting.YELLOW + "Copy resolved profile"))));
        message.appendSibling(copy);
        MWEApi.Chat.addChatMessage(message);
    }

    private void clearState(boolean restoreAliases) {
        List<AliasRecord> aliasesToRestore = new ArrayList<>(sessionAliases.values());
        sessionAliases.clear();
        previousGroupUuids.clear();
        resolvedNames.clear();
        failedLookups.clear();
        pendingLookups.clear();
        announcedResolutions.clear();
        GROUP_RESOLUTIONS.clear();
        NICK_RESOLUTIONS.clear();
        refreshCooldown = 0;

        if (restoreAliases) {
            for (AliasRecord alias : aliasesToRestore) {
                try {
                    String current = MWEApi.Alias.getAlias(alias.uuid, alias.nickName);
                    if (alias.value.equals(current)) {
                        MWEApi.Alias.removeAlias(alias.uuid, alias.nickName);
                        FeatureUtil.refreshName(alias.uuid);
                    }
                } catch (RuntimeException ignored) {}
            }
        }
    }

    private String getGroupingName(NetworkPlayerInfo info) {
        GameProfile profile = info.getGameProfile();
        String formatted = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), profile.getName());
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(formatted);
        if (plain == null) {
            return profile.getName();
        }
        Matcher matcher = USERNAME_PATTERN.matcher(plain);
        String longest = null;
        while (matcher.find()) {
            String candidate = matcher.group();
            if (profile.getName().equalsIgnoreCase(candidate)) {
                return candidate;
            }
            if (longest == null || candidate.length() > longest.length()) {
                longest = candidate;
            }
        }
        return longest == null ? profile.getName() : longest;
    }

    private static Resolution findResolution(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = normalizeName(name);
        Resolution resolution = NICK_RESOLUTIONS.get(key);
        return resolution == null ? GROUP_RESOLUTIONS.get(key) : resolution;
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String normalizeUuid(UUID uuid) {
        return uuid == null ? "" : uuid.toString().replace("-", "");
    }

    private static boolean copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static final class PlayerGroup {
        private final String displayName;
        private final List<NetworkPlayerInfo> players = new ArrayList<>();
        private final Set<UUID> uuids = new HashSet<>();
        private final List<UUID> realUuids = new ArrayList<>();
        private boolean hasNickedPlayer;

        private PlayerGroup(String displayName) {
            this.displayName = displayName;
        }
    }

    private static final class AliasRecord {
        private final UUID uuid;
        private final String nickName;
        private final String value;

        private AliasRecord(UUID uuid, String nickName, String value) {
            this.uuid = uuid;
            this.nickName = nickName;
            this.value = value;
        }
    }

    private static final class Resolution {
        private final String fakeName;
        private final String realName;
        private final UUID realUuid;

        private Resolution(String fakeName, String realName, UUID realUuid) {
            this.fakeName = fakeName;
            this.realName = realName;
            this.realUuid = realUuid;
        }
    }

    public static final class CommandDenick extends MWECommandBase {

        @Override
        public String getCommandName() {
            return "denick";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/denick <debug|copy <fakeName>|status>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 1 && "debug".equalsIgnoreCase(args[0])) {
                ExampleConfig.denickDebugLogging = !ExampleConfig.denickDebugLogging;
                MWEApi.Config.saveConfig();
                FeatureUtil.sendMessage("DeNick", EnumChatFormatting.GOLD,
                        EnumChatFormatting.GRAY + "debug logging "
                                + (ExampleConfig.denickDebugLogging
                                ? EnumChatFormatting.GREEN + "enabled"
                                : EnumChatFormatting.RED + "disabled"));
                return;
            }
            if (args.length == 1 && "status".equalsIgnoreCase(args[0])) {
                FeatureUtil.sendMessage("DeNick", EnumChatFormatting.GOLD,
                        EnumChatFormatting.GRAY + "resolved groups: " + EnumChatFormatting.WHITE
                                + GROUP_RESOLUTIONS.size());
                return;
            }
            if (args.length >= 2 && "copy".equalsIgnoreCase(args[0])) {
                Resolution resolution = findResolution(args[1]);
                if (resolution != null && copyToClipboard(normalizeUuid(resolution.realUuid)
                        + " " + resolution.realName)) {
                    FeatureUtil.sendMessage("DeNick", EnumChatFormatting.GREEN,
                            EnumChatFormatting.GRAY + "copied resolved profile.");
                } else {
                    FeatureUtil.sendMessage("DeNick", EnumChatFormatting.RED,
                            EnumChatFormatting.GRAY + "no resolved profile for " + args[1]);
                }
                return;
            }
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + getCommandUsage(sender)));
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "debug", "copy", "status");
            }
            if (args.length == 2 && "copy".equalsIgnoreCase(args[0])) {
                List<String> names = new ArrayList<>();
                for (Resolution resolution : GROUP_RESOLUTIONS.values()) {
                    names.add(resolution.fakeName);
                }
                Collections.sort(names);
                return getListOfStringsMatchingLastWord(args, names);
            }
            return null;
        }
    }
}
