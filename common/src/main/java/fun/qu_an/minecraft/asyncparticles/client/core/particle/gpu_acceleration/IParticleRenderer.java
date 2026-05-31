package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;

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
	void tick(Vec3 cameraPos, Queue<SingleQuadParticle> particles);

	/**
	 * Called per frame.
	 */
	void compute(Camera camera, float partialTicks);

	/**
	 * Called per frame.
	 */
	void render();

	/**
	 * Appends a new particle to the rendering buffer.
	 * Must be called after tick().
	 * Must be called on main thread.
	 */
	void append(Vec3 cameraPos, SingleQuadParticle tsp);

	void resize(int particleLimit);
}
