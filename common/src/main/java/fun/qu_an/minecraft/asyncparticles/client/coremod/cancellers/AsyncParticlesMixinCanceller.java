package fun.qu_an.minecraft.asyncparticles.client.coremod.cancellers;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import fun.qu_an.minecraft.asyncparticles.client.compat.particle_core.ParticleCoreEarlyCompat;
import fun.qu_an.minecraft.asyncparticles.client.coremod.MixinConfigHelper;

import java.util.List;

public class AsyncParticlesMixinCanceller implements MixinCanceller {
	@Override
	public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
		if (mixinClassName.startsWith("net.caffeinemc.mods.lithium.mixin.collections.entity_by_type")) {
			return MixinConfigHelper.isSafeClassInstanceMultiMap();
		}
		return ParticleCoreEarlyCompat.shouldDisable(mixinClassName);
	}
}
