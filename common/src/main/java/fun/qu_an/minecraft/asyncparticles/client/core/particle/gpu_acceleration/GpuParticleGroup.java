package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Queue;

public class GpuParticleGroup extends QuadParticleGroup implements ParticleGroupAddition {
	private final GpuQuadParticleRenderState renderState;
	private int particleLimit;

	public GpuParticleGroup(ParticleEngine engine, ParticleRenderType particleType) {
		super(engine, particleType);
		this.renderState = (GpuQuadParticleRenderState) particleTypeRenderState;
		this.particleLimit = ConfigHelper.getParticleLimit();
	}

	@Override
	public @NonNull ParticleGroupRenderState extractRenderState(@NonNull Frustum frustum, @NonNull Camera camera, float partialTickTime) {
		renderState.setPartialTick(partialTickTime);
		return renderState;
	}

	public void prepareBuffer() {
		renderState.prepareBuffer();
	}

	public void tickParticles() {
		super.tickParticles();
		asyncparticles$removeDeadParticles();
		renderState.tickRenderers(GpuParticleBehavior.getInstance().getCameraPos(), particles);
	}

	public void flushBufferAndSwap(Vec3 prevGpuCamPos) {
		renderState.flushBufferAndSwap(prevGpuCamPos);
	}

	public void beginFrame() {
		renderState.beginFrame();
	}

	public void resize(int particleLimit) {
		if (this.particleLimit != particleLimit) {
			this.particleLimit = particleLimit;
			Queue<SingleQuadParticle> newParticles = ParticleHelper.newParticleQueue();
			newParticles.addAll(particles);
			particles = newParticles;
		}
		renderState.resize(particleLimit);
	}

	public void reload() {
		renderState.reload();
	}

	public void add(final @NonNull Particle particle) {
		super.add(particle);
		if (ConfigHelper.isAppendNewParticlesToRenderer()) {
			renderState.append(GpuParticleBehavior.getInstance().getCameraPos(), (SingleQuadParticle) particle);
		}
	}
}
