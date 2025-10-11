package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ParticleLayerAttached {
//	private static final AtomicInteger indexCounter = new AtomicInteger(0);
	private static final List<ParticleLayerAttached> attaches = new CopyOnWriteArrayList<>();
//	public final int index = indexCounter.getAndIncrement();
	public final SingleQuadParticle.Layer layer;
	public final QuadParticleRenderState.Storage storage = new QuadParticleRenderState.Storage();

	public ParticleLayerAttached(SingleQuadParticle.Layer layer) {
		this.layer = layer;
		attaches.add(this);
	}

	public static int getParticleCount() {
		return attaches.stream().mapToInt(l -> l.storage.count()).sum();
	}

	public static Map<SingleQuadParticle.Layer, ? extends QuadParticleRenderState.Storage> getParticles() {
		return attaches.stream().filter(l -> l.storage.count() > 0)
			.collect(Collectors.toMap(l -> l.layer, l -> l.storage));
	}

	public void add(float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q, int r, int s) {
		storage.add(f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}
}
