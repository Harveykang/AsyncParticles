package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.particle.SingleQuadParticle;

import java.util.Collection;

public class ComputeData {
	private final Reference2IntMap<SingleQuadParticle.Layer> layerMap = new Reference2IntOpenHashMap<>();
	private ComputeResult result;

	public void clear() {
		layerMap.clear();
		result = null;
	}

	public void add(SingleQuadParticle.Layer layer, int count) {
		if (result != null) {
			throw new IllegalStateException("Cannot update layer while result is present");
		}
		layerMap.merge(layer, count, Integer::sum);
	}

	public void set(SingleQuadParticle.Layer layer, int count) {
		if (result != null) {
			throw new IllegalStateException("Cannot update layer while result is present");
		}
		layerMap.put(layer, count);
	}

	public Collection<SingleQuadParticle.Layer> getLayers() {
		return layerMap.keySet();
	}

	public int getCount(SingleQuadParticle.Layer layer) {
		return layerMap.getInt(layer);
	}

	public int getTotalCount() {
		int count = 0;
		for (int c : layerMap.values()) {
			count += c;
		}
		return count;
	}

	public ComputeResult getResult(GpuBuffer buffer) {
		if (result != null) {
			return result;
		}
		ComputeResult.ParticleSlice[] slices = new ComputeResult.ParticleSlice[layerMap.size()];
//		VertexFormat.Mode quads = VertexFormat.Mode.QUADS;
		int i = 0;
		int baseCount = 0;
		for (Reference2IntMap.Entry<SingleQuadParticle.Layer> entry : layerMap.reference2IntEntrySet()) {
			int count = entry.getIntValue();
			slices[i++] = new ComputeResult.ParticleSlice(entry.getKey(), baseCount, count);
			baseCount += count;
		}
		return result = new ComputeResult(buffer, getTotalCount(), slices);
	}
}
