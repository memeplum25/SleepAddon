package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class EnergyDisplayHudTransformer implements IClassNodeTransformer {

    private static final String TARGET = "fr.alexdoru.mwe.gui.huds.EnergyDisplayHUD";
    private static final String HOOK = "me/standonts/asm/hooks/EnergyDisplayHudHook";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(4);
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("render")) {
                injectTextHooks(method, "appendMeleeHits", status);
            } else if (method.name.equals("renderDummy") && method.desc.equals("()V")) {
                injectTextHooks(method, "appendDummyMeleeHits", status);
            }
        }
    }

    private void injectTextHooks(MethodNode method, String hookMethod,
                                 InjectionCallback status) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!isFinalDisplayString(instruction)) {
                continue;
            }
            method.instructions.insert(instruction, new MethodInsnNode(
                    INVOKESTATIC, HOOK, hookMethod,
                    "(Ljava/lang/String;)Ljava/lang/String;", false));
            status.addInjection();
        }
    }

    private boolean isFinalDisplayString(AbstractInsnNode instruction) {
        if (!(instruction instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) instruction;
        return call.owner.equals("java/lang/StringBuilder")
                && call.name.equals("toString")
                && call.desc.equals("()Ljava/lang/String;")
                || call.owner.equals("java/lang/String")
                && call.name.equals("valueOf")
                && call.desc.equals("(I)Ljava/lang/String;");
    }
}
