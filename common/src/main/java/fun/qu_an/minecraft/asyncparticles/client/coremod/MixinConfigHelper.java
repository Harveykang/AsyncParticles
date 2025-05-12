package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.CONFIG;

public class MixinConfigHelper {
	public static boolean isRedirectFleroviumCulling() {
		return !ModListHelper.SHIMMER_LOADED && CONFIG.isRedirectFleroviumCulling();
	}
}
