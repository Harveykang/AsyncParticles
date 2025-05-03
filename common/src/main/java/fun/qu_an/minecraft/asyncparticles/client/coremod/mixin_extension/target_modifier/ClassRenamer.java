package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import fun.qu_an.minecraft.asyncparticles.client.coremod.PreLaunch;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

@PreLaunch
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
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) insn;
                    if (fin.owner.equals(oldInternalName)) {
                        fin.owner = newInternalName;
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.owner.equals(oldInternalName)) {
                        min.owner = newInternalName;
                    }
                } else if (insn instanceof TypeInsnNode) {
                    TypeInsnNode tin = (TypeInsnNode) insn;
                    tin.desc = tin.desc.replace(oldInternalName, newInternalName);
                }
            }
        }
    }
}
