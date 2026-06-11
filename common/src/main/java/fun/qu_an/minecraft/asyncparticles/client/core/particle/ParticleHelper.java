package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_extract.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import net.minecraft.client.particle.Particle;

import java.util.Queue;

public class ParticleHelper {
	public static <T extends Particle> Queue<T> newParticleQueue() {
		return newParticleQueue(16);
	}

	public static <T extends Particle> Queue<T> newParticleQueue(int size) {
		return IterationSafeEvictingQueue.newInstance(
			size,
			ConfigHelper.getParticleLimit(),
			AsyncTickBehavior.getInstance()::onEvict);
	}

	public static void onClearParticles() {
		AsyncRenderBehavior.getInstance().reset();
		AsyncTickBehavior.getInstance().reset();
		GpuParticleBehavior.INSTANCE.onClearParticles();
	}
}
