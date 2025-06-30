package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.Optional;

public class MixinUtil {
	public static Optional<TargetClassContextExtension> tryAs(ITargetClassContext reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new TargetClassContextExtension(reference));
		}
		return Optional.empty();
	}

	public static String getRefMapperName(String className, String refMapper) {
		if (className.startsWith("forge")) {
			return "forge-" + refMapper;
		}
		if (className.startsWith("fabric")) {
			return "fabric-" + refMapper;
		}
		return refMapper;
	}
}
