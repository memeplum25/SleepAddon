package me.standonts.config;

import fr.alexdoru.configlib.api.ConfigProperty;
import fr.alexdoru.configlib.api.RendererPosition;

public class ExampleConfig {

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Phoenix detector",
            comment = "Detect when an enemy Phoenix uses Resurrection")
    public static boolean phoenixDetector = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Potion detector",
            comment = "Track enemy healing potion usage during deathmatch")
    public static boolean potionDetector = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Potion count on nametags",
            comment = "Show tracked healing potion usage above player nametags")
    public static boolean showPotionUsedOnNametags = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Potion count in tablist",
            comment = "Append tracked healing potion usage to player names in the tablist")
    public static boolean showPotionUsedInTablist = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Final kills on nametags",
            comment = "Show tracked final kill counts above player nametags")
    public static boolean showFinalKillsOnNametags = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "DeNick",
            comment = "Resolve duplicate nick entries through the Mojang profile API")
    public static boolean denick = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "DeNick debug logging",
            comment = "Write detailed DeNick resolution information to the game log")
    public static boolean denickDebugLogging;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Detect extra diamond gear",
            comment = "Detect players carrying diamond equipment that is not part of their class kit")
    public static boolean detectExtraDiamondGear = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Extra diamond gear icons in tablist",
            comment = "Show unexpected diamond gear after player names: sword, helmet, chestplate, leggings and boots")
    public static boolean extraDiamondGearIconsInTablist = true;

    @ConfigProperty(
            category = "Sleep Addon",
            name = "Ghost Block Fix",
            comment = "Refresh a 3x3 render area after server updates while mining dirt or snow (illegal)")
    public static boolean ghostBlockFix = false;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Enable Item Tags",
            comment = "Main switch for item name tags in the world")
    public static boolean itemTagsEnabled = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Phoenix Tears",
            comment = "Show tags for Phoenix's Tears of Regen")
    public static boolean showPhoenixTears = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Squid Absorption",
            comment = "Show tags for Squid's Absorption")
    public static boolean showSquidAbsorption = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Regen-Ade",
            comment = "Show tags for Regen-Ades")
    public static boolean showRegenAde = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Milk Bucket",
            comment = "Show tags for Ultra Pasteurized Milk Buckets")
    public static boolean showMilkBucket = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Diamond Items",
            comment = "Show tags for Diamonds, Diamond Swords and Diamond Armor")
    public static boolean showDiamondItems = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Golden Apples",
            comment = "Show tags for regular and enchanted Golden Apples")
    public static boolean showGoldenApples = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Misc Items",
            comment = "Show tags for Matey and Junk Apples")
    public static boolean showMiscItemTags = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Distance",
            comment = "Show the distance from you to each tagged item")
    public static boolean itemTagsShowDistance = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Count",
            comment = "Show the item stack count, such as x1 or x2")
    public static boolean itemTagsShowCount = true;

    @ConfigProperty(
            category = "Mega Walls",
            subCategory = "Item Tags",
            name = "Show Background",
            comment = "Show a dark background behind item tags")
    public static boolean itemTagsShowBackground = true;

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Mini Scoreboard HUD",
            comment = "Displays a compact Mega Walls scoreboard with timers and team status")
    public static final RendererPosition miniScoreboardPosition =
            new RendererPosition(true, 0.5D, 0.02D);

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Mini Scoreboard background",
            comment = "Draw a dark background behind the Mini Scoreboard HUD")
    public static boolean miniScoreboardBackground = true;

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Team finals on Mini Scoreboard",
            comment = "Include your team's final kills and final assists on the Mini Scoreboard")
    public static boolean miniScoreboardTeamFinals = true;

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Closest Player HUD",
            comment = "Displays the closest visible player from each Mega Walls team")
    public static final RendererPosition closestPlayerPosition =
            new RendererPosition(true, 0.02D, 0.35D);

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Closest Player background",
            comment = "Draw a dark background behind the Closest Player HUD")
    public static boolean closestPlayerBackground = true;

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Closest Strength HUD",
            comment = "Displays nearby Dreadlord and Herobrine Strength durations")
    public static final RendererPosition closestStrengthPosition =
            new RendererPosition(true, 0.78D, 0.35D);

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Skill Slay HUD",
            comment = "Warn when your fully charged ability can execute a nearby enemy")
    public static final RendererPosition skillSlayPosition =
            new RendererPosition(true, 0.5D, 0.55D);

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Leap Mode HUD",
            comment = "Display the current Spider Leap mode: Arrow or Arced")
    public static final RendererPosition leapModePosition =
            new RendererPosition(true, 0.7D, 1.0D);

    @ConfigProperty(
            category = "Sleep Addon",
            subCategory = "HUD",
            name = "Melee hits in Energy HUD",
            comment = "Append the hits needed to reach 100 energy to MWE's Energy HUD")
    public static boolean showMeleeHitsInEnergyHud = true;

    @ConfigProperty(
            category = "Squad",
            subCategory = "HUD",
            name = "Keep your first",
            comment = "Keep your own row at the top of MWE's Squad HUD")
    public static boolean squadHudSelfFirst;

}
