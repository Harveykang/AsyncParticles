package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;

public class DuckParticleRenderer implements IParticleRenderer {
	boolean shouldSkip = false;
	public DuckParticleRenderer(int particleLimit) {
	}

	@Override
	public void beginFrame() {

	}

	@Override
	public void unmapBufferAndSwap() {

	}

	@Override
	public void mapBuffer() {

	}

	@Override
	public void unmapBuffer() {

	}

	@Override
	public boolean isShouldSkip() {
		return false;
	}

	@Override
	public void tick(Vec3 cameraPos, Queue<SingleQuadParticle> particles) {

	}

	@Override
	public void compute(Camera camera, float partialTicks) {

	}

	@Override
	public void render() {

	}

	@Override
	public void append(Vec3 cameraPos, SingleQuadParticle tsp) {

	}

	@Override
	public void resize(int particleLimit) {

	}
}
