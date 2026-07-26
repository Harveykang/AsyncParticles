package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.coremod.adjusters.AdjusterParticlesAsyncTickableGroup;
import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.class_adjuster.MixinClassAdjusterApplication;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class AsyncParticlesMixinErrorHandler implements IMixinErrorHandler {
	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
		return null;
	}

	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
		String className = mixin.getClassName();
		String originalMixin = MixinClassAdjusterApplication.getInstance().getOriginalMixin(className);
		if (AdjusterParticlesAsyncTickableGroup.CLASS_NAME.equals(originalMixin)) {
			return ErrorAction.WARN;
		}
		return null;
	}
}
