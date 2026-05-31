package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// add() -> afterAdd() -> waitFuture() -> submit() -> prepare() -> render()
public class DualAsyncQuadParticleRenderState extends QuadParticleRenderState implements AsyncParticleRenderState {
	private final AsyncQuadParticleRenderState group;
	private final AsyncQuadParticleRenderState translucentGroup;

	public DualAsyncQuadParticleRenderState() {
		group = new AsyncQuadParticleRenderState(false);
		translucentGroup = new AsyncQuadParticleRenderState(true);
	}

	@Override
	public void clear() {
		group.clear();
		translucentGroup.clear();
	}

	@Override
	public void add(@NotNull SingleQuadParticle.Layer layer,
	                float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q,
	                int r, int s) {
		if (layer.translucent()) {
			translucentGroup.add(layer, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
		} else {
			group.add(layer, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
		}
	}

	@Override
	public void afterAdd() {
		group.afterAdd();
		translucentGroup.afterAdd();
		particleCount = group.particleCount + translucentGroup.particleCount;
	}

	@Nullable
	@Override
	public QuadParticleRenderState.PreparedBuffers prepare(final ParticleFeatureRenderer.ParticleBufferCache particleBufferCache, final boolean translucent) {
		if (translucent) {
			return translucentGroup.prepare(particleBufferCache, true);
		} else {
			return group.prepare(particleBufferCache, false);
		}
	}
}
