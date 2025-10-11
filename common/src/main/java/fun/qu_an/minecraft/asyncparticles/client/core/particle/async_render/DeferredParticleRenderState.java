package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;

public class DeferredParticleRenderState implements ParticleGroupRenderState {
	private ParticleGroupRenderState delegate;

	@Override
	public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		if (delegate == null) {
			throw new NullPointerException("Delegate is not set");
		}
		delegate.submit(submitNodeCollector, cameraRenderState);
	}

	@Override
	public void clear() {
		if (delegate == null) {
			throw new NullPointerException("Delegate is not set");
		}
		delegate.clear();
	}

	public ParticleGroupRenderState getDelegate() {
		return delegate;
	}

	public void setDelegate(ParticleGroupRenderState delegate) {
		this.delegate = delegate;
	}
}
