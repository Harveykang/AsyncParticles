package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;

import java.util.Queue;

public class ParticleHelper {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>(ThreadUtil::isOnMainThread);
	public static final ParticleThreadLocal<Boolean> GPU_PARTICLE_PHASE = ParticleThreadLocal.withInitial(ThreadUtil::isOnMainThread, () -> false);

	public static <T extends Particle> Queue<T> newParticleQueue() {
		return newParticleQueue(16);
	}

	public static <T extends Particle> Queue<T> newParticleQueue(int initSize) {
		return IterationSafeEvictingQueue.newInstance(
			initSize,
			ConfigHelper.getParticleLimit(),
			ParticleHelper::onEvict);
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
			&& particle instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp))) {
			addon.asyncparticles$enableLightCache(true);
		}
		Integer i = DESTRUCTION_LIGHT_CACHE.get();
		if (i != null) {
			addon.asyncparticles$setLight(i);
		} else if (b) {
			addon.asyncparticles$refresh();
		} else if (ConfigHelper.isGpuParticles()
			&& particle instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp)){
			// We just use the light cache slot for gpu, but never really enable it.
			int lightColor;
			try {
				lightColor = particle.getLightColor(0f);
			} catch (MissingPaletteEntryException ignore) {
				lightColor = 0;
			}
			addon.asyncparticles$setLight(lightColor);
		}
	}

	public static <T extends Particle> void onEvict(T particle) {
		particle.getParticleGroup().ifPresent(limit -> Minecraft.getInstance().particleEngine.updateCount(limit, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	public static void onClearParticle(Particle particle) {
		try {
			if (particle.isAlive()) {
				particle.remove();
			}
		} catch (Exception ignored) {
		}
	}

	public static void onEvictIgnoreExceptions(Particle particle) {
		particle.getParticleGroup().ifPresent(limit -> Minecraft.getInstance().particleEngine.updateCount(limit, -1));
		try {
			if (particle.isAlive()) {
				particle.remove();
			}
		} catch (Exception ignored) {
		}
	}

	public static void tickGpuParticles(Runnable tickRunnable) {
		GPU_PARTICLE_PHASE.set(true);
		tickRunnable.run();
		GPU_PARTICLE_PHASE.set(false);
	}
}
