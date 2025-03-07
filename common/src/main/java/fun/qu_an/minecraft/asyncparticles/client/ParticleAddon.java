package fun.qu_an.minecraft.asyncparticles.client;


import net.minecraft.world.phys.AABB;

public interface ParticleAddon {
	boolean asyncParticles$shouldRemove();
	void asyncParticles$setTicked();
	boolean asyncParticles$isTicked();
	void asyncedParticles$setRenderSync();
	boolean asyncedParticles$isRenderSync();
	void asyncedParticles$setTickSync();
	boolean asyncedParticles$isTickSync();

	AABB getRenderBoundingBox(float partialTicks);
}
