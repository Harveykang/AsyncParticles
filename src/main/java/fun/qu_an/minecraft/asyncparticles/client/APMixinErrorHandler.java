package fun.qu_an.minecraft.asyncparticles.client;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class APMixinErrorHandler implements IMixinErrorHandler {
	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
//		if (mixin.getClassName().equals("com.moepus.flerovium.mixins.Particle.ParticleEngineMixin")) {
//			return ErrorAction.NONE;
//		}
		return null;
	}

	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
//		if (mixin.getClassName().equals("com.moepus.flerovium.mixins.Particle.ParticleEngineMixin")) {
//			return ErrorAction.NONE;
//		}
		return null;
	}
}
