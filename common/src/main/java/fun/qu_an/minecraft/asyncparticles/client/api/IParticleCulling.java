package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.world.phys.AABB;

public interface IParticleCulling {
	/**
	 * NeoForge getRenderBoundingBox()
	 */
	AABB getRenderBoundingBox(float partialTick);
}
