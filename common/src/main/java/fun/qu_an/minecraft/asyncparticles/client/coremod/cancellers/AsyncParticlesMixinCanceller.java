package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class AsyncParticlesMixinCanceller implements MixinCanceller {
	@Override
	public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
		return switch (mixinClassName) {
			case "net.irisshaders.iris.mixin.fantastic.MixinLevelRenderer",
				 "net.irisshaders.iris.mixin.fabric.MixinLevelRenderer",
				 // disable this because our implementation is better
				 "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
				 // These mixins do not support async rendering
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerRotationMixin",
				 "me.fzzyhmstrs.particle_core.mixins.WorldRendererFrustumMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleManagerCachedLightMixin",
				 "me.fzzyhmstrs.particle_core.mixins.BillboardParticleMixin",
				 "me.fzzyhmstrs.particle_core.mixins.ParticleMixin"
//				 , "net.diebuddies.mixins.ocean.MixinParticleEngine" // Physics mod
				-> true;
			default -> false;
		};
	}
}
