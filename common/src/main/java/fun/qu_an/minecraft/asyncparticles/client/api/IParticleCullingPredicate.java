package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;

public interface IParticleCullingPredicate {
	@ApiStatus.NonExtendable
	default void asyncparticles$setNoCulling() {
		throw new AssertionError("Missing implementation.");
	}

	/**
	 * NeoForge getRenderBoundingBox()
	 */
	AABB getRenderBoundingBox(float partialTick);
}
