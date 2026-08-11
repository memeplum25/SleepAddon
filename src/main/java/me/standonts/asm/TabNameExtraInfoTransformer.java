package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class TabNameExtraInfoTransformer implements IClassNodeTransformer {

    private static final String TARGET = "net.minecraft.client.gui.GuiPlayerTabOverlay";
    private static final String HOOK = "me/standonts/asm/hooks/TabNameExtraInfoHook";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(1);
        for (MethodNode method : classNode.methods) {
            if (isGetPlayerNameMethod(method) && injectSuffixHook(method)) {
                status.addInjection();
                return;
            }
        }
    }

    private boolean isGetPlayerNameMethod(MethodNode method) {
        if (!(method.name.equals("getPlayerName")
                || method.name.equals("func_175243_a")
                || method.name.equals("a"))) {
            return false;
        }

        Type[] arguments = Type.getArgumentTypes(method.desc);
        Type returnType = Type.getReturnType(method.desc);
        return arguments.length == 1
                && arguments[0].getSort() == Type.OBJECT
                && returnType.getSort() == Type.OBJECT
                && returnType.getInternalName().equals("java/lang/String");
    }

    private boolean injectSuffixHook(MethodNode method) {
        String playerInfoDescriptor = Type.getArgumentTypes(method.desc)[0].getDescriptor();
        String hookDescriptor = "(Ljava/lang/String;" + playerInfoDescriptor
                + ")Ljava/lang/String;";
        boolean injected = false;

        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 1));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOK, "appendExtraInfo",
                        hookDescriptor, false));
                method.instructions.insertBefore(instruction, hook);
                injected = true;
            }
        }

        if (injected) {
            method.maxStack = Math.max(method.maxStack, 2);
        }
        return injected;
    }
}
