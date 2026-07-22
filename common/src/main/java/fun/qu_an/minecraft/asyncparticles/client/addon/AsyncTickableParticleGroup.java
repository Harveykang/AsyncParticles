package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;

public interface AsyncTickableParticleGroup extends ParticleGroupAddition {
	/**
	 * invoke condition: ConfigHelper.isAsyncTickParticle() && !ConfigHelper.isGpuOnlyAsyncParticleTick()
	 * && group instanceof AsyncTickableParticleGroup asyncGroup && asyncGroup.asyncparticles$canTickAsync()
	 * which means this.tickParticles() is not called
	 */
	default void asyncparticles$tickSyncParticles(boolean isGpu) {
		throw new AssertionError("Should be implemented");
	}

	/**
	 * invoke condition: ConfigHelper.isAsyncTickParticle() && !ConfigHelper.isGpuOnlyAsyncParticleTick()
	 * && group instanceof AsyncTickableParticleGroup asyncGroup && asyncGroup.asyncparticles$canTickAsync()
	 * which means this.tickParticles() is not called
	 */
	default void asyncparticles$addSync(Particle particle) {
		throw new AssertionError("Should be implemented");
	}

	@Override
	default boolean asyncparticles$canTickAsync() {
		throw new AssertionError("Should be implemented");
	}
}
