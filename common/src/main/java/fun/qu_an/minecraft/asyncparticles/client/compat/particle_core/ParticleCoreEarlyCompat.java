package fun.qu_an.minecraft.asyncparticles.client.compat.particle_core;

import me.fzzyhmstrs.particle_core.PcDisable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ParticleCoreEarlyCompat {
	/**
	 * These mixins conflict with us
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerFrustumMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerRotationMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.WorldRendererFrustumMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerRenderDistanceMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerCountMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.BillboardParticleMixin
	 * while these are compatible with us
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerAsyncMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleBrightnessCacheMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleManagerCachedLightMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleCachePosMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.BufferBuilderVertexMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.FireworksSparkParticleMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleFrustumBlacklistMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.ParticleMoveAdjustMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.WorldRendererDecreaseMixin
	 * @see me.fzzyhmstrs.particle_core.mixins.WorldRendererTypeMixin
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

	public static boolean shouldDisable(String mixinClassName) {
		return "me.fzzyhmstrs.particle_core.mixins.ParticleGroupBrightnessTickMixin".equals(mixinClassName);
	}
}
