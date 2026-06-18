package fun.qu_an.minecraft.asyncparticles.client.addon;

public interface ParticleGroupAddition {
	default void asyncparticles$removeDeadParticles() {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticles$tickSyncParticles() {
		throw new AssertionError("Must be implemented!");
	}
}
