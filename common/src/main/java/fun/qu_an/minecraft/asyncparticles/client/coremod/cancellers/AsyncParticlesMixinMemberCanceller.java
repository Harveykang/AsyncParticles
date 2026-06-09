package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCanceller;

import java.util.List;

public class AsyncParticlesMixinMemberCanceller implements MixinMemberCanceller {
	@Override
	public boolean preCancel(List<String> targetClassNames, String mixinClassName) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin",
				 "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin",
				 "net.diebuddies.mixins.weather.MixinParticleEngine" -> true;
			default -> false;
		};
	}

	@Override
	public boolean shouldCancelMethod(List<String> targetClassNames, String mixinClassName, List<String> targetMethodDescs, String mixinMethodName, String mixinMethodDesc) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.ParticleEngineMixin" ->
				"shouldRenderParticle".equals(mixinMethodName);
			case "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin" ->
				"addCustomRenderTypes".equals(mixinMethodName);
			case "net.diebuddies.mixins.weather.MixinParticleEngine" ->
				"tick".equals(mixinMethodName);
			default -> false;
		};
	}

	@Override
	public boolean shouldCancelField(List<String> targetClassNames, String mixinClassName, String mixinFieldName, String mixinFieldDesc) {
		return false;
	}
}
