package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class TabNameExtraInfoTransformer implements IClassNodeTransformer {

    private static final String TARGET = "fr.alexdoru.mwe.data.PlayerDataManager";
    private static final String PLAYER_DATA = "fr/alexdoru/mwe/data/PlayerDataManager$PlayerData";
    private static final String HOOK = "me/standonts/asm/hooks/TabNameExtraInfoHook";
    private static final String UPDATE_DESCRIPTOR = "(Lcom/mojang/authlib/GameProfile;)L"
            + PLAYER_DATA + ";";
    private static final String PLAYER_DATA_CONSTRUCTOR = "(Lnet/minecraft/util/IChatComponent;"
            + "Lnet/minecraft/util/IChatComponent;CLfr/alexdoru/mwe/api/enums/MWClass;)V";
    private static final String HOOK_DESCRIPTOR = "(Lnet/minecraft/util/IChatComponent;"
            + "Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/util/IChatComponent;";

    @Override
    public String[] getTargetClassName() {
        return new String[]{TARGET};
    }

    @Override
    public void transform(ClassNode classNode, InjectionCallback status) {
        status.setInjectionPoints(1);
        for (MethodNode method : classNode.methods) {
            if ("updatePlayerData".equals(method.name)
                    && UPDATE_DESCRIPTOR.equals(method.desc)
                    && injectDisplayNameHook(method)) {
                status.addInjection();
                return;
            }
        }
    }

    private boolean injectDisplayNameHook(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() != INVOKESPECIAL || !PLAYER_DATA.equals(call.owner)
                    || !"<init>".equals(call.name) || !PLAYER_DATA_CONSTRUCTOR.equals(call.desc)) {
                continue;
            }

            AbstractInsnNode mwClassLoad = previousInstruction(call);
            AbstractInsnNode teamColorLoad = previousInstruction(mwClassLoad);
            AbstractInsnNode displayNameLoad = previousInstruction(teamColorLoad);
            if (!(mwClassLoad instanceof VarInsnNode) || mwClassLoad.getOpcode() != ALOAD
                    || !(teamColorLoad instanceof VarInsnNode) || teamColorLoad.getOpcode() != ILOAD
                    || !(displayNameLoad instanceof VarInsnNode) || displayNameLoad.getOpcode() != ALOAD) {
                return false;
            }

            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOK, "appendExtraInfo",
                    HOOK_DESCRIPTOR, false));
            method.instructions.insert(displayNameLoad, hook);
            method.maxStack = Math.max(method.maxStack, 6);
            return true;
        }
        return false;
    }

    private static AbstractInsnNode previousInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }
}
