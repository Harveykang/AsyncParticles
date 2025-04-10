package fun.qu_an.minecraft.asyncparticles.client.addon;


import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public interface ParticleAddon {
	boolean asyncParticles$shouldRemove();
	void asyncParticles$setTicked();
	boolean asyncParticles$isTicked();
	void asyncedParticles$setRenderSync();
	boolean asyncedParticles$isRenderSync();
	void asyncedParticles$setTickSync();
	boolean asyncedParticles$isTickSync();

	/**
	 * NeoForge getRenderBoundingBox()
	 */
	@NotNull AABB getRenderBoundingBox(float partialTicks);
}
