package me.standonts.asm;

import fr.alexdoru.mwe.api.asm.IClassNodeTransformer;
import fr.alexdoru.mwe.api.asm.InjectionCallback;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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
            if (HookInjector.isVoidMethodWithOneArg(method, "onBlockChange")) {
                inject(method, "onBlockChange");
                status.addInjection();
            } else if (HookInjector.isVoidMethodWithOneArg(method, "onMultiBlockChange")) {
                inject(method, "onMultiBlockChange");
                status.addInjection();
            }
        }
    }

    private void inject(MethodNode method, String hookMethod) {
        String packetDesc = Type.getArgumentTypes(method.desc)[0].getDescriptor();
        HookInjector.callHookAtEntry(method, HOOK, hookMethod, 0, packetDesc);
    }

}
