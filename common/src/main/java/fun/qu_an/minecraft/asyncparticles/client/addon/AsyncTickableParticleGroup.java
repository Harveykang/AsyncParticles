package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;

import java.util.Set;

public interface AsyncTickableParticleGroup extends ParticleGroupAddition {
	default Set<Particle> asyncparticle$getSyncParticles() {
		throw new AssertionError("Should be implemented");
	}

	default void asyncparticle$tickSyncParticles() {
		throw new AssertionError("Should be implemented");
	}

	default void asyncparticle$recordSync(Particle particle) {
		throw new AssertionError("Should be implemented");
	}
}
