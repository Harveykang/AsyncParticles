package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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

	/**
	 * This has way better performance than instanceof/getRenderBoundingBox
	 */
	boolean asyncparticles$shouldCull();

	void asyncparticles$setNoCulling();

	boolean asyncparticles$isVisibleOnScreen();

	void asyncparticles$tickAABBCulling();

	void asyncparticles$tickSphereCulling();

	Class<? extends Particle> asyncparticles$getRealClass();
}
