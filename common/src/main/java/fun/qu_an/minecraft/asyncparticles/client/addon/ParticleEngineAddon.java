package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleEngineAddon {
	void asyncparticles$switchToDeferredQueue();

	void asyncparticles$switchToTickQueue();

	void asyncparticles$addDefers();

	default void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		throw new AssertionError("Must be implemented!");
	}

	void asyncparticles$discardParticlesToAdd();
}
