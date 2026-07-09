package fun.qu_an.minecraft.asyncparticles.client.compat.particle_core;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import me.fzzyhmstrs.particle_core.PcDisable;

import java.util.*;

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
		if (ModListHelper.VS_LOADED) {
			// this will not prevent me.fzzyhmstrs.particle_core.interfaces.BlockPosStorer casting
			// we need a canceler to me.fzzyhmstrs.particle_core.mixins.ParticleMoveAdjustMixin
			set.add("MOVE");
		}
		PcDisable.INSTANCE.getDisabledOptimizations().setDisableOptimizations(List.copyOf(set));
	}
}
