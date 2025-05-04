package fun.qu_an.minecraft.asyncparticles.client.coremod;

import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.MixinInfoExtension;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.Optional;

@PreLaunch
public class MixinUtil {
	public static Optional<TargetClassContextExtension> tryAs(ITargetClassContext reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new TargetClassContextExtension(reference));
		}
		return Optional.empty();
	}

	public static Optional<MixinInfoExtension> tryAs(IMixinInfo reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new MixinInfoExtension(reference));
		}
		return Optional.empty();
	}
}
