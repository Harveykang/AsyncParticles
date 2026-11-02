package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

public class ParticleStormEarlyCompat {
	public static final boolean AVAILABLE = isSupportedVersion() && MixinConfigHelper.isParticleStormMixinEnabled();

	public static boolean isSupportedVersion() {
		return ModListHelper.PARTICLE_STORM_LATER_THAN_1_4_3_1;
	}
}
