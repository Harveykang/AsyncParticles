package fun.qu_an.minecraft.asyncparticles.client.coremod;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;

import java.util.Collection;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.CONFIG;

public class MixinConfigHelper {
	public static Collection<String> getNoCulling() {
		return CONFIG.getNoCulling().stream().filter(ModListHelper::classExists).toList();
	}

	public static Collection<String> getNoLightCache() {
		return CONFIG.getNoLightCache().stream().filter(ModListHelper::classExists).toList();
	}

	public static Collection<String> getLockProvider() {
		return CONFIG.getLockProvider().stream().filter(ModListHelper::classExists).toList();
	}

	public static Collection<String> getLockRequired() {
		return CONFIG.getLockRequired().stream().filter(ModListHelper::classExists).toList();
	}

	public static Collection<String> getReplaceRandom() {
		return CONFIG.getReplaceRandom().stream().filter(ModListHelper::classExists).toList();
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

	public static Collection<String> getContraptionNoParticleCollision() {
		return CONFIG.getContraptionNoParticleCollision().stream().filter(ModListHelper::classExists).toList();
	}
}
