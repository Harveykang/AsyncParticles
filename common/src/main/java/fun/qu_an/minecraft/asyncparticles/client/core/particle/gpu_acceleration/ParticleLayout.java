package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.particle.ParticleRenderType;

import java.util.Map;

public record ParticleLayout(int totalCount, Map<IParticleRenderKey, Slice> slices) {
	public static ParticleLayout of(int totalCount, Map<IParticleRenderKey, Slice> slices) {
		return new ParticleLayout(totalCount, slices);
	}

	public int totalVertexCount() {
		return totalCount * 4;
	}

	public int totalIndexCount() {
		return totalCount * 6;
	}

	public record Slice(ParticleRenderType layer, int baseCount, int count) {
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
