package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster;

import org.objectweb.asm.tree.ClassNode;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull requests are merged.
 */
public interface MixinClassProvider {
	/**
	 * The class node will be renamed to a generated name.
	 */
	ClassNode getMixinClass();
}
