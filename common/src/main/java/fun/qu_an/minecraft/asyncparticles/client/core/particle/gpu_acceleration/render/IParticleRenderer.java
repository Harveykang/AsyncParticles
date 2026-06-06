package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

public interface IParticleRenderer {
	void beginFrame();

	void unmapBufferAndSwap();

	void mapBuffer();

	void unmapBuffer();

	boolean isShouldSkip();

	/**
	 * Called per tick.
	 * Can be called on non-main thread.
	 */
	void tick(Vec3 cameraPos, Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particles);

	/**
	 * Called per frame.
	 */
	ComputeResult compute(Camera camera, float partialTicks);

	/**
	 * Appends a new particle to the rendering buffer.
	 * Must be called after tick().
	 * Must be called on main thread.
	 */
	void append(Vec3 cameraPos, SingleQuadParticle tsp);

	void resize(int particleLimit);

	Collection<SingleQuadParticle.Layer> getLayers();
}
