package me.standonts.hud;

import me.standonts.config.ExampleConfig;
import fr.alexdoru.mwe.api.MWEApi;
import fr.alexdoru.mwe.api.enums.MWClass;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings.GameType;

public final class SkillSlayHUD extends AddonHud {

    private static final double MAX_VERTICAL_DIFF = 2.2D;
    private static final String DISPLAY_TEXT = EnumChatFormatting.RED.toString()
            + EnumChatFormatting.BOLD + "EXECUTE READY";
    private boolean couldSlay;

    public SkillSlayHUD() {
        super(ExampleConfig.skillSlayPosition);
    }

    @Override
    public void render(ScaledResolution resolution) {
        boolean canSlay = false;
        if (MC.thePlayer != null && MC.theWorld != null && MC.thePlayer.experienceLevel >= 100) {
            MWClass myClass = resolveClass(MC.thePlayer);
            canSlay = supportsSkillSlay(myClass) && getBestExecutableTarget(myClass) != null;
        }
        if (canSlay) {
            if (!couldSlay) {
                MC.thePlayer.playSound("random.orb", 1.0F, 2.0F);
            }
            int width = MC.fontRendererObj.getStringWidth(DISPLAY_TEXT);
            getPosition().updateAdjustedAbsolutePosition(resolution, width, MC.fontRendererObj.FONT_HEIGHT);
            int x = getPosition().getAbsoluteRenderX();
            int y = getPosition().getAbsoluteRenderY();
            MC.fontRendererObj.drawStringWithShadow(DISPLAY_TEXT, x, y, 0xFFFFFF);
        }
        couldSlay = canSlay;
    }

    @Override
    public void renderDummy() {
        int x = getPosition().getAbsoluteRenderX();
        int y = getPosition().getAbsoluteRenderY();
        MC.fontRendererObj.drawStringWithShadow(DISPLAY_TEXT, x, y, 0xFFFFFF);
    }

    @Override
    public boolean isEnabled(long currentTimeMillis) {
        boolean enabled = getPosition().isEnabled() && MC.thePlayer != null
                && MWEApi.Scoreboard.getScoreboardParser().isInMwGame();
        if (!enabled) {
            couldSlay = false;
        }
        return enabled;
    }

    private boolean supportsSkillSlay(MWClass mwClass) {
        return mwClass == MWClass.DREADLORD || mwClass == MWClass.GOLEM
                || mwClass == MWClass.HEROBRINE || mwClass == MWClass.PIRATE
                || mwClass == MWClass.MOLEMAN;
    }

    private EntityPlayer getBestExecutableTarget(MWClass mwClass) {
        EntityPlayer best = null;
        float lowestHealth = Float.MAX_VALUE;
        double damage = getSkillDamage(mwClass);
        for (EntityPlayer target : MC.theWorld.playerEntities) {
            if (target == MC.thePlayer || target.isDead || target.getHealth() <= 0.0F
                    || !isEnemy(target) || isSpectator(target) || !isInSkillHitbox(mwClass, target)) {
                continue;
            }
            float effectiveHealth = target.getHealth() + target.getAbsorptionAmount();
            if (effectiveHealth <= damage && effectiveHealth < lowestHealth) {
                lowestHealth = effectiveHealth;
                best = target;
            }
        }
        return best;
    }

    private boolean isInSkillHitbox(MWClass mwClass, EntityPlayer target) {
        switch (mwClass) {
            case HEROBRINE:
                return isInRadius(target, 5.0D);
            case GOLEM:
                return isInRadius(target, 4.5D);
            case PIRATE:
                return isInForwardCone(target, 14.0D, 22.0D);
            case DREADLORD:
                return isInForwardCone(target, 15.0D, 18.0D);
            case MOLEMAN:
                return isInMoleDashPath(target);
            default:
                return false;
        }
    }

    private boolean isInRadius(EntityPlayer target, double radius) {
        return MC.thePlayer.getDistanceToEntity(target) <= radius
                && Math.abs(target.posY - MC.thePlayer.posY) <= MAX_VERTICAL_DIFF;
    }

    private boolean isInForwardCone(EntityPlayer target, double range, double halfAngleDegrees) {
        if (MC.thePlayer.getDistanceToEntity(target) > range
                || Math.abs(target.posY - MC.thePlayer.posY) > MAX_VERTICAL_DIFF) {
            return false;
        }
        Vec3 look = MC.thePlayer.getLook(1.0F).normalize();
        Vec3 direction = new Vec3(
                target.posX - MC.thePlayer.posX,
                target.posY + target.getEyeHeight() * 0.5D
                        - (MC.thePlayer.posY + MC.thePlayer.getEyeHeight() * 0.5D),
                target.posZ - MC.thePlayer.posZ).normalize();
        double dot = Math.max(-1.0D, Math.min(1.0D, look.dotProduct(direction)));
        return Math.toDegrees(Math.acos(dot)) <= halfAngleDegrees;
    }

    private boolean isInMoleDashPath(EntityPlayer target) {
        if (Math.abs(target.posY - MC.thePlayer.posY) > MAX_VERTICAL_DIFF
                || MC.thePlayer.getDistanceToEntity(target) > 10.0F) {
            return false;
        }
        Vec3 start = new Vec3(MC.thePlayer.posX, MC.thePlayer.posY + 1.0D, MC.thePlayer.posZ);
        Vec3 look = getHorizontalLook();
        Vec3 end = start.addVector(look.xCoord * 9.0D, 0.0D, look.zCoord * 9.0D);
        Vec3 targetCenter = new Vec3(target.posX,
                target.posY + target.getEyeHeight() * 0.5D, target.posZ);
        Vec3 toTarget = targetCenter.subtract(start);
        double forward = toTarget.dotProduct(look);
        return distancePointToSegment(targetCenter, start, end) <= 1.9D
                && forward >= 0.0D && forward <= 9.5D;
    }

    private MWClass resolveClass(EntityPlayer player) {
        MWClass mwClass = MWEApi.Player.getPlayerInfo(player).getMWClass();
        return mwClass == null ? MWClass.ofPlayer(player.getName()) : mwClass;
    }

    private boolean isEnemy(EntityPlayer target) {
        char myTeam = MWEApi.Player.getPlayerInfo(MC.thePlayer).getPlayerTeamColor();
        char targetTeam = MWEApi.Player.getPlayerInfo(target).getPlayerTeamColor();
        if (myTeam != '\0' && targetTeam != '\0') {
            return myTeam != targetTeam;
        }
        return !MC.thePlayer.isOnSameTeam(target);
    }

    private boolean isSpectator(EntityPlayer target) {
        if (MC.getNetHandler() == null) {
            return false;
        }
        NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(target.getUniqueID());
        return info != null && info.getGameType() == GameType.SPECTATOR;
    }

    private Vec3 getHorizontalLook() {
        double yaw = Math.toRadians(MC.thePlayer.rotationYaw);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw)).normalize();
    }

    private double distancePointToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.dotProduct(segment);
        if (lengthSquared <= 1.0E-6D) {
            return point.subtract(start).lengthVector();
        }
        double t = point.subtract(start).dotProduct(segment) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 projection = start.addVector(segment.xCoord * t, segment.yCoord * t, segment.zCoord * t);
        return point.subtract(projection).lengthVector();
    }

    private double getSkillDamage(MWClass mwClass) {
        switch (mwClass) {
            case HEROBRINE:
            case PIRATE:
                return 5.0D;
            case GOLEM:
                return 6.0D;
            case DREADLORD:
            case MOLEMAN:
                return 8.0D;
            default:
                return 0.0D;
        }
    }
}
