package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;

public interface IParticleRenderer extends Closeable {
	void beginFrame(float deltaPartialTick);

	/**
	 * Called per tick.
	 * Called on main thread.
	 */
	void flushBufferAndSwap(Vec3 prevGpuCamPos);

	/**
	 * Called per tick.
	 * Called on main thread.
	 */
	void prepareBuffer();

	boolean isMapped();

	boolean isShouldSkip();

	/**
	 * Called per tick.
	 * Called on non-main thread.
	 */
	<T extends Collection<SingleQuadParticle>> void tick(Vec3 cameraPos, Map<SingleQuadParticle.Layer, T> particles);

	/**
	 * Called multiple per frame, but only computed once.
	 * Called on main thread.
	 */
	void compute(Camera camera, float partialTicks);

	/**
	 * Ensures the previously submitted compute dispatch has completed.
	 * Called on the render thread, right before the GPU particle draw.
	 */
	ComputeResult awaitCompute();

	/**
	 * Appends a new particle to the rendering buffer.
	 * Must be called after tick().
	 * Must be called on main thread.
	 */
	void append(Vec3 cameraPos, SingleQuadParticle tsp);

	void resize(int particleLimit);

	Collection<SingleQuadParticle.Layer> getComputeLayers();

	void reset();

	@Override
	void close();
}
