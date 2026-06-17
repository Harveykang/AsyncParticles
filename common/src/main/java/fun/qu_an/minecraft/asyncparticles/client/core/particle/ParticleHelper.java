package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.client.particle.Particle;

import java.util.Queue;

public class ParticleHelper {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>(RenderSystem::isOnRenderThread);

	public static <T extends Particle> Queue<T> newParticleQueue() {
		return newParticleQueue(16);
	}

	public static <T extends Particle> Queue<T> newParticleQueue(int initSize) {
		return IterationSafeEvictingQueue.newInstance(
			initSize,
			AsyncParticlesConfig.MAX_PARTICLE_LIMIT,
			AsyncTickBehavior.getInstance()::onEvict);
	}

	public static<T extends Particle> Queue<T> newBusyWaitParticleQueue() {
		return newBusyWaitParticleQueue(16);
	}

	public static<T extends Particle> Queue<T> newBusyWaitParticleQueue(int initSize) {
		return BusyWaitEvictingQueue.newInstance(initSize,
			AsyncParticlesConfig.MAX_PARTICLE_LIMIT,
			AsyncTickBehavior.getInstance()::onEvict);
	}

	public static void onClearParticles() {
		AsyncTickBehavior.getInstance().reset();
		GpuParticleBehavior.getInstance().onClearParticles();
		if (ModListHelper.A_GOOD_PLACE_LOADED) {
			AGoodPlaceCompat.onParticleEngineClear();
		}
	}
}
