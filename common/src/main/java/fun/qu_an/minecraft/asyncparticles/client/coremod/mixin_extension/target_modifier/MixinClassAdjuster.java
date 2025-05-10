package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.function.Supplier;

public interface MixinClassAdjuster {
	/**
	 * @return the name of the mixin class that should apply the class adjuster to
	 */
	String getMixinClassName();

	/**
	 * @param originalTargets the list of original target class names
	 * @return null or originalTargets to cancel applying the class adjuster,
	 * otherwise a list of modified target class names
	 * @apiNote Will not be obfuscated, runtime names will be used
	 */
	List<String> getTargets(List<String> originalTargets);

	/**
	 * @return the new priority of the mixin class, lower priority will be applied first
	 * null to keep the original priority
	 * @see Mixin#priority()
	 */
	default Integer getPriority() {
		return null;
	}

	/**
	 * @return the name of the refmap config file to use for this class adjuster,
	 * or null to use the default refmap
	 * @see IMixinConfigPlugin#getRefMapperConfig()
	 */
	@Nullable String getRefMapperConfig();

	/**
	 * Once a mixin class has been modified by a class adjuster,
	 * it will ignore the result of {@link IMixinConfigPlugin#shouldApplyMixin(String, String)}.
	 * You can use this method to check if the class adjuster should apply to the target class.
	 *
	 * @see IMixinConfigPlugin#shouldApplyMixin(String, String)
	 */
	default boolean shouldApplyMixin(String targetClassName) {
		return true;
	}

	/**
	 * Adjust anything special, e.g. {@link Opcodes#INVOKESPECIAL}
	 *
	 * @param mixinClassNode provide a remapped class node
	 * @return a new ClassNode or the modified toModifyClassNode
	 * @apiNote DO NOT modify targets here
	 */
	@ApiStatus.Experimental
	default ClassNode adjustMixin(String targetClassName, Supplier<ClassNode> mixinClassNode) {
		return null;
	}

	default void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	default void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
