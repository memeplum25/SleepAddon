package me.standonts.features;

import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DiamondGearDetector {

    private static final int DETECT_INTERVAL_TICKS = 2;
    private static final int BIT_SWORD = 1;
    private static final int BIT_HELMET = 1 << 1;
    private static final int BIT_CHESTPLATE = 1 << 2;
    private static final int BIT_LEGGINGS = 1 << 3;
    private static final int BIT_BOOTS = 1 << 4;

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<MWClass, Integer> CLASS_GEAR = createClassGear();
    private static final Map<UUID, Integer> EXTRA_GEAR = new HashMap<>();
    private static final Map<String, UUID> UUID_BY_NAME = new HashMap<>();
    private static final Set<UUID> ALERTED_PLAYERS = new HashSet<>();

    private int tickCounter;
    private boolean active;

    public static String getExtraDiamondIcons(String playerName) {
        if (!ExampleConfig.detectExtraDiamondGear || playerName == null) {
            return "";
        }
        UUID uuid = UUID_BY_NAME.get(playerName.toLowerCase(Locale.ROOT));
        Integer mask = uuid == null ? null : EXTRA_GEAR.get(uuid);
        return mask == null ? "" : iconsFor(mask);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean shouldRun = ExampleConfig.detectExtraDiamondGear
                && MC.theWorld != null
                && MC.thePlayer != null
                && MWEApi.Scoreboard.getScoreboardParser().isInMwGame();
        if (!shouldRun) {
            if (active) {
                clearState();
            }
            return;
        }

        active = true;
        if (++tickCounter % DETECT_INTERVAL_TICKS == 0) {
            scanPlayers();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == MC.theWorld) {
            clearState();
        }
    }

    private void scanPlayers() {
        Set<UUID> visiblePlayers = new HashSet<>();
        UUID_BY_NAME.clear();

        for (EntityPlayer player : MC.theWorld.playerEntities) {
            if (player == null || player.isDead || player.getUniqueID() == null) {
                continue;
            }
            UUID uuid = player.getUniqueID();
            visiblePlayers.add(uuid);
            UUID_BY_NAME.put(player.getName().toLowerCase(Locale.ROOT), uuid);

            MWClass mwClass = getPlayerClass(player);
            Integer expectedGear = mwClass == null ? null : CLASS_GEAR.get(mwClass);
            if (expectedGear == null) {
                EXTRA_GEAR.remove(uuid);
                continue;
            }

            int oldMask = EXTRA_GEAR.containsKey(uuid) ? EXTRA_GEAR.get(uuid) : 0;
            int newMask = findUnexpectedGear(player, expectedGear);
            EXTRA_GEAR.put(uuid, newMask);
            if (newMask != 0 && oldMask == 0 && ALERTED_PLAYERS.add(uuid)) {
                sendAlert(player, newMask);
            }
        }

        EXTRA_GEAR.keySet().retainAll(visiblePlayers);
        ALERTED_PLAYERS.retainAll(visiblePlayers);
    }

    private MWClass getPlayerClass(EntityPlayer player) {
        MWClass mwClass = MWEApi.Player.getPlayerInfo(player).getMWClass();
        if (mwClass == null) {
            mwClass = MWClass.ofPlayer(player.getUniqueID());
        }
        return mwClass == null ? MWClass.ofPlayer(player.getName()) : mwClass;
    }

    private int findUnexpectedGear(EntityPlayer player, int expectedGear) {
        int gear = 0;
        if (isItem(player.getEquipmentInSlot(0), Items.diamond_sword)) {
            gear |= BIT_SWORD;
        }
        if (isItem(player.getEquipmentInSlot(4), Items.diamond_helmet)) {
            gear |= BIT_HELMET;
        }
        if (isItem(player.getEquipmentInSlot(3), Items.diamond_chestplate)) {
            gear |= BIT_CHESTPLATE;
        }
        if (isItem(player.getEquipmentInSlot(2), Items.diamond_leggings)) {
            gear |= BIT_LEGGINGS;
        }
        if (isItem(player.getEquipmentInSlot(1), Items.diamond_boots)) {
            gear |= BIT_BOOTS;
        }
        return gear & ~expectedGear;
    }

    private static boolean isItem(ItemStack stack, Item item) {
        return stack != null && stack.getItem() == item;
    }

    private static void sendAlert(EntityPlayer player, int mask) {
        String playerName = ScorePlayerTeam.formatPlayerName(player.getTeam(), player.getName());
        FeatureUtil.sendMessage("Diamond Gear", EnumChatFormatting.AQUA,
                playerName + EnumChatFormatting.GRAY + " has extra diamond gear: " + iconsFor(mask));
    }

    private static String iconsFor(int mask) {
        if (mask == 0) {
            return "";
        }
        StringBuilder icons = new StringBuilder()
                .append(EnumChatFormatting.AQUA)
                .append(EnumChatFormatting.BOLD);
        if ((mask & BIT_SWORD) != 0) icons.append('\u24c8');
        if ((mask & BIT_HELMET) != 0) icons.append('\u24bd');
        if ((mask & BIT_CHESTPLATE) != 0) icons.append('\u24b8');
        if ((mask & BIT_LEGGINGS) != 0) icons.append('\u24c1');
        if ((mask & BIT_BOOTS) != 0) icons.append('\u24b7');
        return icons.toString();
    }

    private void clearState() {
        tickCounter = 0;
        active = false;
        EXTRA_GEAR.clear();
        UUID_BY_NAME.clear();
        ALERTED_PLAYERS.clear();
    }

    private static Map<MWClass, Integer> createClassGear() {
        EnumMap<MWClass, Integer> gear = new EnumMap<>(MWClass.class);
        put(gear, MWClass.ANGEL, BIT_LEGGINGS);
        put(gear, MWClass.ARCANIST, BIT_SWORD | BIT_LEGGINGS);
        put(gear, MWClass.ASSASSIN, BIT_SWORD);
        put(gear, MWClass.AUTOMATON, BIT_LEGGINGS | BIT_BOOTS);
        put(gear, MWClass.BLAZE, BIT_SWORD);
        put(gear, MWClass.COW, BIT_CHESTPLATE);
        put(gear, MWClass.CREEPER, BIT_LEGGINGS);
        put(gear, MWClass.DRAGON, BIT_HELMET);
        put(gear, MWClass.DREADLORD, BIT_SWORD | BIT_HELMET);
        put(gear, MWClass.ENDERMAN, BIT_BOOTS);
        put(gear, MWClass.GOLEM, BIT_CHESTPLATE | BIT_BOOTS);
        put(gear, MWClass.HEROBRINE, BIT_SWORD);
        put(gear, MWClass.HUNTER, BIT_HELMET);
        put(gear, MWClass.MOLEMAN, BIT_LEGGINGS);
        put(gear, MWClass.PHOENIX, 0);
        put(gear, MWClass.PIGMAN, BIT_SWORD | BIT_CHESTPLATE);
        put(gear, MWClass.PIRATE, BIT_HELMET | BIT_BOOTS);
        put(gear, MWClass.RENEGADE, BIT_BOOTS);
        put(gear, MWClass.SHAMAN, BIT_SWORD | BIT_BOOTS);
        put(gear, MWClass.SHARK, BIT_SWORD | BIT_BOOTS);
        put(gear, MWClass.SHEEP, BIT_LEGGINGS);
        put(gear, MWClass.SKELETON, BIT_HELMET);
        put(gear, MWClass.SNOWMAN, BIT_SWORD | BIT_LEGGINGS);
        put(gear, MWClass.SPIDER, BIT_SWORD | BIT_BOOTS);
        put(gear, MWClass.SQUID, BIT_BOOTS);
        put(gear, MWClass.WEREWOLF, BIT_SWORD | BIT_CHESTPLATE);
        put(gear, MWClass.ZOMBIE, BIT_CHESTPLATE);
        return gear;
    }

    private static void put(Map<MWClass, Integer> gear, MWClass mwClass, int mask) {
        gear.put(mwClass, mask);
    }
}
