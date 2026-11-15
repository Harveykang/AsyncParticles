package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;

import java.util.Queue;

public interface GpuParticleGroup extends ParticleGroupAddition, AsyncTickableParticleGroup {
	Queue<SingleQuadParticle> asyncparticles$getGpuParticles();

	QuadParticleRenderState asyncparticles$clearCpuRenderState();

	void asyncparticles$removeDeadGpuParticles();
}
