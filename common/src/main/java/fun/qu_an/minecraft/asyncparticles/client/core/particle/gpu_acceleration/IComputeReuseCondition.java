package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.Camera;

public interface IComputeReuseCondition {
	IComputeReuseCondition DUMMY = (camera, partialTicks) -> true;

	/**
	 * Returns whether the current view can reuse the prepared compute result.
	 */
	boolean matches(Camera camera, float partialTicks);
}
