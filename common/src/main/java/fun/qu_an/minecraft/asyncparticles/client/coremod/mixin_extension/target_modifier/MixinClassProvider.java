package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import org.objectweb.asm.tree.ClassNode;

public interface MixinClassProvider {
	/**
	 * The class node will be renamed to a generated name.
	 */
	ClassNode getMixinClass();
}
