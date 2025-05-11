package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import org.objectweb.asm.tree.ClassNode;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull request.
 */
public interface MixinClassProvider {
	/**
	 * The class node will be renamed to a generated name.
	 */
	ClassNode getMixinClass();
}
