package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleAddon {
	void asyncparticles$setTicked();
	void asyncparticles$resetTicked();
	boolean asyncparticles$isTicked();
	void asyncparticles$setRenderSync();
	boolean asyncparticles$isRenderSync();

	/**
	 * Forge shouldCull()
	 */
	boolean shouldCull();

	boolean asyncparticles$isVisibleOnScreen();

	@SuppressWarnings({"unchecked", "rawtypes"})
	default <T extends Particle> Class<T> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}
}
