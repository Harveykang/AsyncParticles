package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.particle.ParticleRenderType;

import java.util.Map;

public record ComputeResult(int totalCount, Map<ParticleRenderType, ParticleSlice> slices, int maxCount) {
	public static ComputeResult of(int totalCount, Map<ParticleRenderType, ParticleSlice> slices) {
		int maxCount = 0;
		for (ParticleSlice slice : slices.values()) {
			if (slice.count() > maxCount) {
				maxCount = slice.count();
			}
		}
		return new ComputeResult(totalCount, slices, maxCount);
	}

	public int totalVertexCount() {
		return totalCount * 4;
	}

	public int totalIndexCount() {
		return totalCount * 6;
	}

	public int maxIndexCount() {
		return maxCount * 6;
	}

	public record ParticleSlice(ParticleRenderType layer, int baseCount, int count) {
		public int vertexOffset() {
			return baseCount * 4;
		}

		public int vertexCount() {
			return count * 4;
		}

		public int indexCount() {
			return count * 6;
		}

		public int indexOffset() {
			return baseCount * 6;
		}
	}
}
