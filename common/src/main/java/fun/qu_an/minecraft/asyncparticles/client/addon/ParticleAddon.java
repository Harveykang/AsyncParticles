package fun.qu_an.minecraft.asyncparticles.client.addon;

import fun.qu_an.minecraft.asyncparticles.client.api.IParticleCullingPredicate;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleAddon extends IParticleCullingPredicate {
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
