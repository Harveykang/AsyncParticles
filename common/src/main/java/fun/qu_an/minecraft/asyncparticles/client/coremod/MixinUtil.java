package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.Optional;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.classExists;

public class MixinUtil {
	public static Optional<TargetClassContextExtension> tryAs(ITargetClassContext reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new TargetClassContextExtension(reference));
		}
		return Optional.empty();
	}

	public static String getMixinClassName(String className) {
		return classExists(className) ? className : (IS_FORGE ? "forge." : "fabric.") + className;
	}

	public static String getRefMapperName(String className, String refMapper) {
		return classExists(className) ? refMapper : (IS_FORGE ? "forge-" : "fabric-") + refMapper;
	}
}
