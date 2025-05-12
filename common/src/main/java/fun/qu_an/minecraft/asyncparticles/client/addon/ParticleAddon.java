package fun.qu_an.minecraft.asyncparticles.client.addon;

public interface ParticleAddon {
	void asyncparticles$setTicked();
	void asyncparticles$resetTicked();
	boolean asyncparticles$isTicked();
	void asyncparticles$setRenderSync();
	boolean asyncparticles$isRenderSync();
	void asyncparticles$setTickSync();
	boolean asyncparticles$isTickSync();

	/**
	 * Forge shouldCull()
	 */
	boolean shouldCull();
}
