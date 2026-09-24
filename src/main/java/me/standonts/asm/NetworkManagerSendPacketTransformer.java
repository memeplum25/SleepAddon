package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class NetworkManagerSendPacketTransformer implements IClassNodeTransformer {

    private static final String TARGET = "net.minecraft.network.NetworkManager";
    private static final String HOOK = "me/standonts/asm/hooks/GhostBlockFixHook";
    private static final String PACKET_CLASS = "net/minecraft/network/Packet";
    private static final String OBFUSCATED_PACKET_CLASS = "ff";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(1);
        for (MethodNode method : classNode.methods) {
            if (!isSendPacket(method)) {
                continue;
            }
            String packetDesc = Type.getArgumentTypes(method.desc)[0].getDescriptor();
            HookInjector.callHookAtEntry(method, HOOK, "onSentPacket", 1, packetDesc);
            status.addInjection();
            return;
        }
    }

    private boolean isSendPacket(MethodNode method) {
        if (!isSendPacketName(method.name)) {
            return false;
        }
        Type[] arguments = Type.getArgumentTypes(method.desc);
        if (arguments.length != 1 || arguments[0].getSort() != Type.OBJECT
                || Type.getReturnType(method.desc).getSort() != Type.VOID) {
            return false;
        }
        String packetName = arguments[0].getInternalName();
        return packetName.equals(PACKET_CLASS) || packetName.equals(OBFUSCATED_PACKET_CLASS);
    }

    private boolean isSendPacketName(String methodName) {
        return methodName.equals("sendPacket")
                || methodName.equals("func_179290_a")
                || methodName.equals("a");
    }

}
