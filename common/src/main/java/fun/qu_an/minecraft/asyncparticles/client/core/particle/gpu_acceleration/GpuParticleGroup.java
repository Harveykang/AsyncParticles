package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public class GpuParticleGroup extends QuadParticleGroup implements ParticleGroupAddition {
	private final GpuQuadParticleRenderState renderState;

	public GpuParticleGroup(ParticleEngine engine, ParticleRenderType particleType) {
		super(engine, particleType);
		this.renderState = (GpuQuadParticleRenderState) particleTypeRenderState;
	}

	@Override
	public @NonNull ParticleGroupRenderState extractRenderState(@NonNull Frustum frustum, @NonNull Camera camera, float partialTickTime) {
		renderState.setPartialTick(partialTickTime);
		return renderState;
	}

	public void mapBuffers() {
		renderState.mapBuffers(() -> {
			Set<SingleQuadParticle.Layer> potentialLayer = new ReferenceOpenHashSet<>();
			for (SingleQuadParticle sqp : particles) {
				potentialLayer.add(sqp.getLayer());
			}
			return potentialLayer;
		});
	}

	public void tickParticles() {
		super.tickParticles();
		asyncparticles$removeDeadParticles();
		renderState.tickRenderers(GpuParticleBehavior.INSTANCE.getCameraPos(), particles);
	}

	public void unmapBuffersAndSwap(Vec3 prevGpuCamPos) {
		renderState.unmapBuffersAndSwap(prevGpuCamPos);
	}

	public void beginFrame() {
		renderState.beginFrame();
	}

	public void append(Vec3 camPos, SingleQuadParticle particle) {
		renderState.append(camPos, particle);
	}

	public void resize(int particleLimit) {
		renderState.resize(particleLimit);
	}

	public void reload() {
		renderState.reload();
	}

	public void add(final @NonNull Particle particle) {
		super.add(particle);
		if (ConfigHelper.isAppendNewParticlesToRenderer()) {
			renderState.append(GpuParticleBehavior.INSTANCE.getCameraPos(), (SingleQuadParticle) particle);
		}
	}
}
