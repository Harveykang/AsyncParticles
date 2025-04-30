package fun.qu_an.minecraft.asyncparticles.client.coremod;

import java.util.Collection;

import static fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinConfig.CONFIG;

public class MixinConfigHelper {
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
}
