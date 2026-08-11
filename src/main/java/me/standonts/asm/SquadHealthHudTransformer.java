package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class SquadHealthHudTransformer implements IClassNodeTransformer {

    private static final String TARGET = "fr.alexdoru.mwe.gui.huds.SquadHealthHUD";
    private static final String HOOK = "me/standonts/asm/hooks/SquadHealthHudHook";
    private static final String RENDERER_DESC = "Lfr/alexdoru/configlib/api/IRenderer;";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(3);

        for (MethodNode method : classNode.methods) {
            if (isRenderMethod(method)) {
                replaceRender(method);
                status.addInjection();
            } else if (checkMethodNode(method, "renderDummy", "()V")) {
                replaceNoArgMethod(method, "renderDummy");
                status.addInjection();
            } else if (checkMethodNode(method, "isEnabled", "(J)Z")) {
                replaceIsEnabled(method);
                status.addInjection();
            }
        }
    }

    private boolean isRenderMethod(MethodNode method) {
        return method.name.equals("render")
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.VOID;
    }

    private void replaceRender(MethodNode method) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 1));
        instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, "render",
                "(" + RENDERER_DESC + method.desc.substring(1), false));
        instructions.add(new InsnNode(RETURN));
        replaceMethodBody(method, instructions, 2, 2);
    }

    private void replaceNoArgMethod(MethodNode method, String hookMethod) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, hookMethod,
                "(" + RENDERER_DESC + ")V", false));
        instructions.add(new InsnNode(RETURN));
        replaceMethodBody(method, instructions, 1, 1);
    }

    private void replaceIsEnabled(MethodNode method) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(LLOAD, 1));
        instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, "isEnabled",
                "(" + RENDERER_DESC + "J)Z", false));
        instructions.add(new InsnNode(IRETURN));
        replaceMethodBody(method, instructions, 3, 3);
    }

    private void replaceMethodBody(MethodNode method, InsnList instructions,
                                   int maxStack, int maxLocals) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }
        method.instructions.add(instructions);
        method.maxStack = maxStack;
        method.maxLocals = maxLocals;
    }
}
