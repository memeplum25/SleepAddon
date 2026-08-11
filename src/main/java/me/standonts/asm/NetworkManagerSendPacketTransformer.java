package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class NetworkManagerSendPacketTransformer implements IClassNodeTransformer {

    private static final String TARGET = "net.minecraft.network.NetworkManager";
    private static final String HOOK = "me/standonts/asm/hooks/GhostBlockFixHook";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(1);
        for (MethodNode method : classNode.methods) {
            if (isSendPacket(method)) {
                Type packetType = Type.getArgumentTypes(method.desc)[0];
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 1));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOK, "onSentPacket",
                        "(" + packetType.getDescriptor() + ")V", false));
                method.instructions.insertBefore(method.instructions.getFirst(), hook);
                method.maxStack = Math.max(method.maxStack, 1);
                status.addInjection();
                return;
            }
        }
    }

    private boolean isSendPacket(MethodNode method) {
        if (!(method.name.equals("sendPacket")
                || method.name.equals("func_179290_a")
                || method.name.equals("a"))) {
            return false;
        }
        Type[] arguments = Type.getArgumentTypes(method.desc);
        if (arguments.length != 1 || arguments[0].getSort() != Type.OBJECT
                || Type.getReturnType(method.desc).getSort() != Type.VOID) {
            return false;
        }
        String packetName = arguments[0].getInternalName();
        return packetName.equals("net/minecraft/network/Packet") || packetName.equals("ff");
    }
}
