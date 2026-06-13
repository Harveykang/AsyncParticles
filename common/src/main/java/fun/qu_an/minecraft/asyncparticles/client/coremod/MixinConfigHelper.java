package fun.qu_an.minecraft.asyncparticles.client.coremod;

import java.util.Collection;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.CONFIG;

public class MixinConfigHelper {
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

	public static boolean isSafeBlockEntityMap() {
		return CONFIG.isSafeBlockEntityMap();
	}

	public static Collection<String> getAsyncTickableParticleGroups() {
		return CONFIG.getAsyncTickableParticleGroups();
	}

	public static Collection<String> getModifyFromParticleMethod() {
		return CONFIG.getModifyFromParticleMethod();
	}
}
