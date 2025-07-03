package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;

import java.util.Collection;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.CONFIG;

public class MixinConfigHelper {
	public static boolean isRedirectFleroviumCulling() {
		return !ModListHelper.SHIMMER_LOADED && CONFIG.isRedirectFleroviumCulling();
	}

	public static Collection<String> getNoCulling() {
		return CONFIG.getNoCulling();
	}

	public static Collection<String> getNoLightCache() {
		return CONFIG.getNoLightCache();
	}

	public static Collection<String> getLockProvider() {
		return CONFIG.getLockProvider();
	}

	public static Collection<String> getLockRequired() {
		return CONFIG.getLockRequired();
	}

	public static Collection<String> getReplaceRandom() {
		return CONFIG.getReplaceRandom();
	}

	public static boolean isSafeLegacyRandomSource() {
		return CONFIG.isSafeLegacyRandomSource();
	}

	public static boolean isSafeClassInstanceMultiMap() {
		return CONFIG.isSafeClassInstanceMultiMap();
	}
}
