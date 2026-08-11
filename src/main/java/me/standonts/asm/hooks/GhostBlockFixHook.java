package me.standonts.asm.hooks;

import me.standonts.features.GhostBlockFix;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;

public final class GhostBlockFixHook {

    private GhostBlockFixHook() {}

    public static void onSentPacket(Packet<?> packet) {
        try {
            GhostBlockFix.onSentPacket(packet);
        } catch (Throwable ignored) {}
    }

    public static void onBlockChange(S23PacketBlockChange packet) {
        try {
            GhostBlockFix.onServerBlockChange(packet);
        } catch (Throwable ignored) {}
    }

    public static void onMultiBlockChange(S22PacketMultiBlockChange packet) {
        try {
            GhostBlockFix.onServerMultiBlockChange(packet);
        } catch (Throwable ignored) {}
    }
}
