package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleEngineAddon {
	default void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticle$tickSyncParticles() {
		throw new AssertionError("Must be implemented!");
	}
}
