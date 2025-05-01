package fun.qu_an.minecraft.asyncparticles.client.mixin_extension;

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
}
