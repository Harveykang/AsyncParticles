package fun.qu_an.minecraft.asyncparticles.client.core.fabric;

import net.minecraft.world.phys.AABB;

public class GameUtilImpl {
	private static final AABB INFINITE_AABB = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

	public static AABB infinityAABB() {
		return INFINITE_AABB;
	}
}
