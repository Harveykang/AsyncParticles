package fun.qu_an.minecraft.asyncparticles.client.core;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import net.minecraft.client.particle.Particle;

import java.util.Queue;

public class ParticleHelper {
	public static <T extends Particle> Queue<T> newParticleQueue() {
		return IterationSafeEvictingQueue.newInstance(
			16,
			ConfigHelper.getParticleLimit(),
			AsyncTickBehavior::onEvict);
	}
}
