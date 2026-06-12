package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCanceller;

import java.util.List;

public class AsyncParticlesMixinMemberCanceller implements MixinMemberCanceller {
	@Override
	public boolean preCancel(List<String> targetClassNames, String mixinClassName) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin",
				 "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin",
				 "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
				 "net.diebuddies.mixins.weather.MixinParticleEngine",
				 "qouteall.imm_ptl.core.mixin.client.particle.MixinParticleEngine",
				 "foundry.veil.mixin.performance.client.PerformanceLevelRendererMixin" -> true;
			default -> false;
		};
	}

	@Override
	public boolean shouldCancelMethod(List<String> targetClassNames, String mixinClassName, List<String> targetMethodDescs, String mixinMethodName, String mixinMethodDesc) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin" ->
				"shouldRenderParticle".equals(mixinMethodName); // FIXME how can i get it work...
			// See mixin.fabric.porting_lib_base.MixinMixinParticleEngine
			case "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin" ->
				"addCustomRenderTypes".equals(mixinMethodName);
			case "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin" ->
				"skipGeneratingAABB".equals(mixinMethodName) || "FastFrustumCheck".equals(mixinMethodName);
			case "net.diebuddies.mixins.weather.MixinParticleEngine" -> "tick".equals(mixinMethodName);
			case "qouteall.imm_ptl.core.mixin.client.particle.MixinParticleEngine" ->
				"onTickParticle".equals(mixinMethodName);
			case "foundry.veil.mixin.performance.client.PerformanceLevelRendererMixin" ->
				"clearParticlesColor".equals(mixinMethodName);
			default -> false;
		};
	}

	@Override
	public boolean shouldCancelField(List<String> targetClassNames, String mixinClassName, String mixinFieldName, String mixinFieldDesc) {
		return false;
	}
}
