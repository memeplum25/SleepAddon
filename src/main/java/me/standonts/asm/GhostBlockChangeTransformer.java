package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class GhostBlockChangeTransformer implements IClassNodeTransformer {

    private static final String TARGET =
            "fr.alexdoru.mwe.asm.hooks.mc.network.NetHandlerPlayClientHook_BlockChangeListener";
    private static final String HOOK = "me/standonts/asm/hooks/GhostBlockFixHook";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(2);
        for (MethodNode method : classNode.methods) {
            if (isPacketHook(method, "onBlockChange")) {
                inject(method, "onBlockChange");
                status.addInjection();
            } else if (isPacketHook(method, "onMultiBlockChange")) {
                inject(method, "onMultiBlockChange");
                status.addInjection();
            }
        }
    }

    private boolean isPacketHook(MethodNode method, String name) {
        return method.name.equals(name)
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.VOID;
    }

    private void inject(MethodNode method, String hookMethod) {
        Type packetType = Type.getArgumentTypes(method.desc)[0];
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(ALOAD, 0));
        hook.add(new MethodInsnNode(INVOKESTATIC, HOOK, hookMethod,
                "(" + packetType.getDescriptor() + ")V", false));
        method.instructions.insertBefore(method.instructions.getFirst(), hook);
        method.maxStack = Math.max(method.maxStack, 1);
    }
}
