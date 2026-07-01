package fun.qu_an.minecraft.asyncparticles.client.compat.particle_core;

import me.fzzyhmstrs.particle_core.PcDisable;

import java.util.*;

public class ParticleCoreEarlyCompat {
	/**
	 * These mixins conflict with us
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerFrustumMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerRotationMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.WorldRendererFrustumMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerRenderDistanceMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerCountMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.BillboardParticleMixin",
	 * while these are compatible with us
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerAsyncMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleBrightnessCacheMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleManagerCachedLightMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleCachePosMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.BufferBuilderVertexMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.FireworksSparkParticleMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleFrustumBlacklistMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.ParticleMoveAdjustMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.WorldRendererDecreaseMixin",
	 * "me.fzzyhmstrs.particle_core.mixins.WorldRendererTypeMixin"
	 */
	public static void initEarly() {
		List<String> disableOptimizations = PcDisable.INSTANCE.getDisabledOptimizations().getDisableOptimizations();
		Set<String> set = new LinkedHashSet<>(disableOptimizations.size() + 10);
		set.addAll(disableOptimizations);
		set.add("COUNT");
		set.add("CULLING");
		set.add("ROTATION");
		set.add("LIGHTMAP");
		PcDisable.INSTANCE.getDisabledOptimizations().setDisableOptimizations(List.copyOf(set));
	}
}
