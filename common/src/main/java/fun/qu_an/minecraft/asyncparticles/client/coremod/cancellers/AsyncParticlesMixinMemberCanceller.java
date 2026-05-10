package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller.MixinMemberCanceller;

import java.util.List;

public class AsyncParticlesMixinMemberCanceller implements MixinMemberCanceller {
	@Override
	public boolean preCancel(List<String> targetClassNames, String mixinClassName) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
				 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin",
				 "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin",
				 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
				 "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin",
				 "net.diebuddies.mixins.weather.MixinParticleEngine" -> true;
			default -> false;
		};
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public boolean shouldCancelMethod(List<String> targetClassNames, String mixinClassName, List<String> targetMethodDescs, String mixinMethodName, String mixinMethodDesc) {
		return switch (mixinClassName) {
			case "einstein.subtle_effects.mixin.client.particle.FabricParticleEngineMixin",
				 "einstein.subtle_effects.mixin.client.particle.ForgeParticleEngineMixin" ->
				"shouldRenderParticle".equals(mixinMethodName);
			case "team.teampotato.ruok.mixin.minecraft.ParticleManagerMixin" ->
				"tick".equals(mixinMethodName); // TODO update RuOK to a new version
			case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" ->
				"flerovium$getLightColorCached".equals(mixinMethodName);
			case "io.github.fabricators_of_create.porting_lib.mixin.client.ParticleEngineMixin" ->
				"port_lib$addCustomRenderTypes".equals(mixinMethodName);
			case "net.diebuddies.mixins.weather.MixinParticleEngine" ->
				"tick".equals(mixinMethodName);
			default -> false;
		};
	}

	@Override
	public boolean shouldCancelField(List<String> targetClassNames, String mixinClassName, String mixinFieldName, String mixinFieldDesc) {
		return switch (mixinClassName) {
			case "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin" ->
				"flerovium$lastTick".equals(mixinFieldName) ||
				"flerovium$cachedLight".equals(mixinFieldName);
			default -> false;
		};
	}
}
