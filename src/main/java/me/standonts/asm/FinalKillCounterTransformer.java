package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class FinalKillCounterTransformer implements IClassNodeTransformer {

    private static final String TARGET = "fr.alexdoru.mwe.features.FinalKillCounter";
    private static final String PROCESS_MESSAGE_DESCRIPTOR = "(Lnet/minecraftforge/client/event/"
            + "ClientChatReceivedEvent;Ljava/lang/String;Ljava/lang/String;)Z";
    private static final String HOOK = "me/standonts/asm/hooks/FinalKillCounterHook";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(1);
        for (MethodNode method : classNode.methods) {
            if (!"processMessage".equals(method.name)
                    || !PROCESS_MESSAGE_DESCRIPTOR.equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == INVOKEVIRTUAL
                        && "java/util/regex/Matcher".equals(call.owner)
                        && "find".equals(call.name) && "()Z".equals(call.desc)) {
                    method.instructions.insert(call, new MethodInsnNode(INVOKESTATIC, HOOK,
                            "allowMessageMatch", "(Z)Z", false));
                    status.addInjection();
                    return;
                }
            }
        }
    }
}
