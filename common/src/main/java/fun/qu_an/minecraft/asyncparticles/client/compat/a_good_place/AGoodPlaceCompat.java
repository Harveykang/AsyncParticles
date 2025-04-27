//package fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place;
//
//import fun.qu_an.minecraft.asyncparticles.client.mixin.a_good_place.InvokerBlocksParticlesManager;
//import nl.enjarai.a_good_place.particles.BlocksParticlesManager;
//
//public class AGoodPlaceCompat {
//	public static void onParticleEngineClear() {
//		InvokerBlocksParticlesManager.accessor_getHiddenBlocks()
//			.forEach(InvokerBlocksParticlesManager::invoker_markBlockForRender);
//		BlocksParticlesManager.clear();
//	}
//}
