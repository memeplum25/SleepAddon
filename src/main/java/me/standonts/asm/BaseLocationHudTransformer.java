package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class BaseLocationHudTransformer implements IClassNodeTransformer {

    private static final String TARGET = "fr.alexdoru.mwe.gui.huds.BaseLocationHUD";
    private static final String HOOK = "me/standonts/asm/hooks/BaseLocationHudHook";
    private static final String RENDERER_DESC = "Lfr/alexdoru/configlib/api/IRenderer;";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(2);
        String mapDescriptor = findCurrentMapDescriptor(classNode);
        if (mapDescriptor == null) {
            return;
        }

        for (MethodNode method : classNode.methods) {
            if (isRenderMethod(method)) {
                replaceRender(classNode, method, mapDescriptor);
                status.addInjection();
            } else if (checkMethodNode(method, "renderDummy", "()V")) {
                replaceRenderDummy(method);
                status.addInjection();
            }
        }
    }

    private String findCurrentMapDescriptor(ClassNode classNode) {
        for (FieldNode field : classNode.fields) {
            if (field.name.equals("currentMap")) {
                return field.desc;
            }
        }
        return null;
    }

    private boolean isRenderMethod(MethodNode method) {
        return method.name.equals("render")
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.VOID;
    }

    private void replaceRender(ClassNode classNode, MethodNode method, String mapDescriptor) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new FieldInsnNode(GETFIELD, classNode.name, "currentMap", mapDescriptor));
        instructions.add(new VarInsnNode(ALOAD, 1));
        instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, "render",
                "(" + RENDERER_DESC + mapDescriptor + method.desc.substring(1), false));
        instructions.add(new InsnNode(RETURN));
        replaceMethodBody(method, instructions, 3, 2);
    }

    private void replaceRenderDummy(MethodNode method) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, "renderDummy",
                "(" + RENDERER_DESC + ")V", false));
        instructions.add(new InsnNode(RETURN));
        replaceMethodBody(method, instructions, 1, 1);
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
