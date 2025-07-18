package fun.qu_an.minecraft.asyncparticles.client.compat.create;

public interface ContraptionEntityAddon {
	default boolean asyncparticles$doParticleCollision() {
		throw new AssertionError("Missing implementation.");
	}
}
