package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;

import java.util.Queue;

public class ParticleHelper {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>();

	public static <T extends Particle> Queue<T> newParticleQueue() {
		return IterationSafeEvictingQueue.newInstance(
			16,
			ConfigHelper.getParticleLimit(),
			AsyncTickBehavior.INSTANCE::onEvicted);
	}

	public static void doFirstRefresh(Particle particle) {
		LightCachedParticleAddon addon = (LightCachedParticleAddon) particle;
		boolean b = ConfigHelper.particleLightCache() && !addon.asyncparticles$isStaticLight();
		if (b) {
			addon.asyncparticles$enableLightCache();
		}
		Integer i = DESTRUCTION_LIGHT_CACHE.get();
		if (i != null) {
			addon.asyncparticles$setLight(i);
		} else if (b) {
			addon.asyncparticles$refresh();
		} else if (ConfigHelper.isGpuParticles()
			&& particle instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.INSTANCE.canRenderFast(tsp)){
			// We just use the light cache slot for gpu, but never really enable it.
			addon.asyncparticles$setLight(particle.getLightColor(0f));
		}
	}
}
