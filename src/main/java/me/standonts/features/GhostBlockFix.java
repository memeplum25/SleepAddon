package me.standonts.features;

import fr.alexdoru.mwe.api.MWEApi;
import me.standonts.config.ExampleConfig;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GhostBlockFix {

    private static final long PENDING_TIMEOUT_NANOS = 2_000_000_000L;
    private static final long MIN_REFRESH_INTERVAL_NANOS = 300_000_000L;
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final AtomicBoolean REFRESH_QUEUED = new AtomicBoolean();

    private static volatile PendingDig pendingDig;
    private static volatile long lastRefreshAt;

    public static void onSentPacket(Packet<?> packet) {
        if (!ExampleConfig.ghostBlockFix || !(packet instanceof C07PacketPlayerDigging)) {
            return;
        }

        C07PacketPlayerDigging digging = (C07PacketPlayerDigging) packet;
        C07PacketPlayerDigging.Action action = digging.getStatus();
        BlockPos position = digging.getPosition();
        if (action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
            pendingDig = isSupportedBlock(position)
                    ? new PendingDig(position, System.nanoTime() + PENDING_TIMEOUT_NANOS)
                    : null;
        } else if (action == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
                || action == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
            PendingDig pending = pendingDig;
            if (pending != null && pending.position.equals(position)) {
                pending.expiresAt = System.nanoTime() + PENDING_TIMEOUT_NANOS;
            }
        }
    }

    public static void onServerBlockChange(S23PacketBlockChange packet) {
        if (packet != null && isNearPendingPosition(packet.getBlockPosition())) {
            queueRefresh(pendingDig);
        }
    }

    public static void onServerMultiBlockChange(S22PacketMultiBlockChange packet) {
        if (!ExampleConfig.ghostBlockFix || packet == null) {
            return;
        }
        for (S22PacketMultiBlockChange.BlockUpdateData update : packet.getChangedBlocks()) {
            if (isNearPendingPosition(update.getPos())) {
                queueRefresh(pendingDig);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == MC.theWorld) {
            reset();
        }
    }

    private static boolean isNearPendingPosition(BlockPos changedPosition) {
        if (!ExampleConfig.ghostBlockFix || changedPosition == null) {
            return false;
        }
        PendingDig pending = pendingDig;
        if (pending == null || System.nanoTime() > pending.expiresAt) {
            pendingDig = null;
            return false;
        }
        return Math.abs(changedPosition.getX() - pending.position.getX()) <= 1
                && changedPosition.getY() == pending.position.getY()
                && Math.abs(changedPosition.getZ() - pending.position.getZ()) <= 1;
    }

    private static void queueRefresh(final PendingDig pending) {
        if (pending == null || !REFRESH_QUEUED.compareAndSet(false, true)) {
            return;
        }
        try {
            MWEApi.Tasks.queueSyncDelayedTask(() -> {
                try {
                    long now = System.nanoTime();
                    if (ExampleConfig.ghostBlockFix && MC.theWorld != null
                            && pending == pendingDig && now <= pending.expiresAt
                            && now - lastRefreshAt >= MIN_REFRESH_INTERVAL_NANOS) {
                        MC.theWorld.markBlockRangeForRenderUpdate(
                                pending.position.add(-1, 0, -1),
                                pending.position.add(1, 0, 1));
                        lastRefreshAt = now;
                    }
                    if (pending == pendingDig) {
                        pendingDig = null;
                    }
                } finally {
                    REFRESH_QUEUED.set(false);
                }
            }, 1);
        } catch (Throwable ignored) {
            REFRESH_QUEUED.set(false);
        }
    }

    private static boolean isSupportedBlock(BlockPos position) {
        if (MC.theWorld == null || position == null) {
            return false;
        }
        Block block = MC.theWorld.getBlockState(position).getBlock();
        return block == Blocks.dirt || block == Blocks.snow;
    }

    private static void reset() {
        pendingDig = null;
        lastRefreshAt = 0L;
        REFRESH_QUEUED.set(false);
    }

    private static final class PendingDig {
        private final BlockPos position;
        private volatile long expiresAt;

        private PendingDig(BlockPos position, long expiresAt) {
            this.position = position;
            this.expiresAt = expiresAt;
        }
    }
}
