package fun.qu_an.minecraft.asyncparticles.client.core;

import fun.qu_an.minecraft.asyncparticles.client.core.render.AsyncQuadParticleRenderState;
import fun.qu_an.minecraft.asyncparticles.client.core.render.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import org.jetbrains.annotations.NotNull;

public class AsyncQuadParticleGroup extends QuadParticleGroup {
	public AsyncQuadParticleGroup(ParticleEngine particleEngine, ParticleRenderType particleRenderType) {
		super(particleEngine, particleRenderType);
	}
	private static final boolean renderParallel = false;

	@Override
	public @NotNull ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float f) {
		getRenderState().dispatch(() -> {
			if (renderParallel){

			} else {
				super.extractRenderState(frustum, camera, f);
			}
			getRenderState().afterAdd();
		}, AsyncRenderBehavior.EXECUTOR);
		return getRenderState();
	}

	public AsyncQuadParticleRenderState getRenderState() {
		return (AsyncQuadParticleRenderState) particleTypeRenderState;
	}

	public ParticleRenderType getParticleType() {
		return particleType;
	}
}
