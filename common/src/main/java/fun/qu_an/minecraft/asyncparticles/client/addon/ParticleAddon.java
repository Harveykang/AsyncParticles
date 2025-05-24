package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;

public interface ParticleAddon {
	void asyncparticles$setTicked();
	void asyncparticles$resetTicked();
	boolean asyncparticles$isTicked();
	void asyncparticles$setRenderSync();
	boolean asyncparticles$isRenderSync();
	void asyncparticles$setTickSync();
	boolean asyncparticles$isTickSync();

	/**
	 * NeoForge getRenderBoundingBox()
	 */
	AABB getRenderBoundingBox(float partialTick);

	Class<? extends Particle> asyncparticles$getRealClass();
}
