package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;

import java.util.Queue;

public class ParticleHelper {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>(RenderSystem::isOnRenderThread);

	public static <T extends Particle> Queue<T> newParticleQueue() {
		return newParticleQueue(16);
	}

	public static <T extends Particle> Queue<T> newParticleQueue(int initSize) {
		return IterationSafeEvictingQueue.newInstance(
			initSize,
			AsyncParticlesConfig.MAX_PARTICLE_LIMIT);
	}

	public static<T extends Particle> Queue<T> newBusyWaitParticleQueue() {
		return newBusyWaitParticleQueue(16);
	}

	public static<T extends Particle> Queue<T> newBusyWaitParticleQueue(int initSize) {
		return BusyWaitEvictingQueue.newInstance(initSize,
			AsyncParticlesConfig.MAX_PARTICLE_LIMIT);
	}

	public static void onClearParticles() {
		AsyncTickBehavior.getInstance().reset();
		GpuParticleBehavior.getInstance().onClearParticles();
		if (ModListHelper.A_GOOD_PLACE_LOADED) {
			AGoodPlaceCompat.onParticleEngineClear();
		}
		if (ModListHelper.PARTICLERAIN_LOADED) {
			ParticleRainCompat.onParticleEngineClear();
		}
	}

	public static void doFirstRefresh(Particle particle) {
		LightCachedParticleAddon addon = (LightCachedParticleAddon) particle;
		boolean b = ConfigHelper.particleLightCache() && !addon.asyncparticles$isStaticLight();
		if (b && !(ConfigHelper.isGpuParticles()
			&& particle instanceof SingleQuadParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp))) {
			addon.asyncparticles$enableLightCache(true);
		}
		Integer i = DESTRUCTION_LIGHT_CACHE.get();
		if (i != null) {
			addon.asyncparticles$setLight(i);
		} else if (b) {
			addon.asyncparticles$refresh();
		} else if (ConfigHelper.isGpuParticles()
			&& particle instanceof SingleQuadParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp)) {
			// We just use the light cache slot for gpu, but never really enable it.
			int lightColor;
			try {
				lightColor = addon.asyncparticles$invoke_getLightCoords(0f);
			} catch (MissingPaletteEntryException ignore) {
				lightColor = 0;
			}
			addon.asyncparticles$setLight(lightColor);
		}
	}
}
