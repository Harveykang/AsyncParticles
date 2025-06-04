package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster;

import org.objectweb.asm.tree.*;

import java.util.Iterator;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull requests are merged.
 */
public class ClassRenamer {
    public static void renameClass(ClassNode classNode, String newClassName) {
        String oldInternalName = classNode.name;
        String newInternalName = newClassName.replace('.', '/');
        classNode.name = newInternalName;

        for (FieldNode field : classNode.fields) {
            field.desc = field.desc.replace(oldInternalName, newInternalName);
        }

        for (MethodNode method : classNode.methods) {
            method.desc = method.desc.replace(oldInternalName, newInternalName);

            Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();
                if (insn instanceof FieldInsnNode fin) {
					if (fin.owner.equals(oldInternalName)) {
                        fin.owner = newInternalName;
                    }
                } else if (insn instanceof MethodInsnNode min) {
					if (min.owner.equals(oldInternalName)) {
                        min.owner = newInternalName;
                    }
                } else if (insn instanceof TypeInsnNode tin) {
					tin.desc = tin.desc.replace(oldInternalName, newInternalName);
                }
            }

            if (method.localVariables!= null) {
                for (LocalVariableNode localVariableNode : method.localVariables) {
                    localVariableNode.desc = localVariableNode.desc.replace(oldInternalName, newInternalName);
                }
            }
        }
    }
}
