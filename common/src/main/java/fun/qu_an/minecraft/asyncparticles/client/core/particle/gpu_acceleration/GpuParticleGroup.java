package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
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

	public void unmapBuffersAndSwap() {
		renderState.unmapBuffersAndSwap();
	}

	public void beginFrame() {
		renderState.beginFrame();
	}

	public void append(SingleQuadParticle particle) {
		renderState.append(GpuParticleBehavior.INSTANCE.getCameraPos(), particle);
	}

	public void resize(int particleLimit) {
		renderState.resize(particleLimit);
	}
}
