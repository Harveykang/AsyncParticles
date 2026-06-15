package fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place;

import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.a_good_place.AccessorBlocksParticlesManager;
import nl.enjarai.a_good_place.particles.BlocksParticlesManager;

public class AGoodPlaceCompat {
	public static void onParticleEngineClear() {
		AccessorBlocksParticlesManager.accessor_getHiddenBlocks()
			.forEach(AccessorBlocksParticlesManager::invoker_markBlockForRender);
		BlocksParticlesManager.clear();
	}
}
