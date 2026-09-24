package me.standonts.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Bytecode helpers shared by the addon's class transformers.
 */
public final class HookInjector {

    /** Descriptor of the renderer interface implemented by the hooked MWE HUDs. */
    public static final String RENDERER_DESC = "Lfr/alexdoru/configlib/api/IRenderer;";

    private HookInjector() {}

    /** Returns true if the method is a HUD render method that only takes the scaled resolution. */
    public static boolean isRenderMethod(MethodNode method) {
        return method.name.equals("render")
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.VOID;
    }

    public static boolean isVoidMethodWithOneArg(MethodNode method, String name) {
        return method.name.equals(name)
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.VOID;
    }

    public static void replaceBody(MethodNode method, InsnList instructions,
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

    public static void insertAtEntry(MethodNode method, InsnList instructions) {
        final AbstractInsnNode first = method.instructions.getFirst();
        if (first == null) {
            method.instructions.add(instructions);
        } else {
            method.instructions.insertBefore(first, instructions);
        }
        method.maxStack = Math.max(method.maxStack, 1);
    }

    /**
     * Calls a static hook method at the start of the given method, forwarding
     * the argument held in the given local variable.
     */
    public static void callHookAtEntry(MethodNode method, String hookOwner, String hookName,
                                       int argumentLocal, String argumentDesc) {
        String descriptor = "(" + argumentDesc + ")V";
        if (hasStaticCall(method, hookOwner, hookName, descriptor)) {
            return;
        }
        final InsnList hook = new InsnList();
        hook.add(new VarInsnNode(ALOAD, argumentLocal));
        hook.add(new MethodInsnNode(INVOKESTATIC, hookOwner, hookName,
                descriptor, false));
        insertAtEntry(method, hook);
    }

    public static boolean hasStaticCall(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == INVOKESTATIC && owner.equals(call.owner)
                        && name.equals(call.name) && descriptor.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

}
