package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.renderer.state.level.QuadParticleRenderState;

public interface ParticleVertexStorageAddition {
	ParticleSlice asyncparticles$slice(int start, int end);

	@FunctionalInterface
	interface ParticleSlice {
		void forEachParticle(QuadParticleRenderState.ParticleConsumer consumer);
	}
}
