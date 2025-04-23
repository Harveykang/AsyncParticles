package fun.qu_an.minecraft.asyncparticles.client.addon;


import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public interface ParticleAddon {
	boolean asyncparticles$shouldRemove();
	void asyncparticles$setTicked();
	boolean asyncparticles$isTicked();
	void asyncparticles$setRenderSync();
	boolean asyncparticles$isRenderSync();
	void asyncparticles$setTickSync();
	boolean asyncparticles$isTickSync();

	/**
	 * NeoForge getRenderBoundingBox()
	 */
	@NotNull AABB getRenderBoundingBox(float partialTicks);
}
