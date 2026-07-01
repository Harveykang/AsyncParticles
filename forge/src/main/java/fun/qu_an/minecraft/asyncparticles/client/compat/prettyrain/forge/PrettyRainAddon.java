package fun.qu_an.minecraft.asyncparticles.client.compat.prettyrain.forge;

import net.minecraft.world.phys.AABB;

public interface PrettyRainAddon {
	AABB asyncparticles$getWeatherAABB();

	void asyncparticles$setWeatherAABB(AABB aabb);

	boolean asyncparticles$invisible();

	void asyncparticles$setInvisible(boolean visible);
}
