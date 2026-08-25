package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

import java.util.List;

public class AsyncParticlesMixinCanceller implements MixinCanceller {
	@Override
	public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
		if (mixinClassName.startsWith("me.jellysquid.mods.lithium.mixin.collections.entity_by_type")) {
			return MixinConfigHelper.isSafeClassInstanceMultiMap();
		}
		return switch (mixinClassName) {
			case "com.moepus.flerovium.mixins.Particle.ParticleEngineMixin",
				 "com.moepus.flerovium.mixins.Particle.ParticleMixin",
//				 "net.diebuddies.mixins.ocean.MixinParticleEngine", // Physics mod
				 "indi.yunherry.weather.mixin.MixinParticle",
				 "ca.fxco.moreculling.mixin.WorldRenderer_rainMixin",
				 "forge.me.thosea.badoptimizations.mixin.MixinParticleManager",
				 "fabric.me.thosea.badoptimizations.mixin.MixinParticleManager"
				-> true;
			// see fun.qu_an.minecraft.asyncparticles.client.compat.particle_core.ParticleCoreEarlyCompat#initEarly
			case "me.fzzyhmstrs.particle_core.mixins.ParticleMoveAdjustMixin" -> ModListHelper.VS_LOADED;
			default -> false;
		};
	}
}
