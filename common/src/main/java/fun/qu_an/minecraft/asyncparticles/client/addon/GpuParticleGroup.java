package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.SingleQuadParticle;

import java.util.Queue;

public interface GpuParticleGroup {
	Queue<SingleQuadParticle> asyncparticles$getGpuParticles();

	void asyncparticles$removeDeadGpuParticles();
}
