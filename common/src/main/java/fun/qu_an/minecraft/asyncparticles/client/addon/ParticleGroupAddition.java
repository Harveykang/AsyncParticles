package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;

public interface ParticleGroupAddition {
	default void asyncparticles$tickParticles(boolean isGpu) {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticles$removeDeadParticles() {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticles$onClearParticles() {
		throw new AssertionError("Must be implemented!");
	}

	default boolean asyncparticles$canTickAsync() {
		return false;
	}

	default boolean asyncparticles$isSyncParticle(Particle particle) {
		return false;
	}
}
