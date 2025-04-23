package fun.qu_an.minecraft.asyncparticles.client.addon;

public interface ParticleAddon {
	boolean asyncparticles$shouldRemove();
	void asyncparticles$setTicked();
	boolean asyncparticles$isTicked();
	void asyncparticles$setRenderSync();
	boolean asyncparticles$isRenderSync();
	void asyncparticles$setTickSync();
	boolean asyncparticles$isTickSync();

	/**
	 * Forge shouldCull()
	 */
	default boolean shouldCull() {
		return true;
	}
}
