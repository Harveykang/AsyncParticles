package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.Optional;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;

public class MixinUtil {
	public static Optional<TargetClassContextExtension> tryAs(ITargetClassContext reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new TargetClassContextExtension(reference));
		}
		return Optional.empty();
	}

	public static String getRefMapperName(String className, String refMapper) {
		return IS_FORGE ? null : className.startsWith("fabric") ? "fabric-" + refMapper : refMapper;
	}
}
