package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.particle.SingleQuadParticle;

public record ComputeResult(GpuBuffer buffer, int totalCount, ParticleSlice[] slices,
                            GpuBuffer indirectBuffer, int layerCount, int indirectCommandStride) {
	public static ComputeResult of(GpuBuffer buffer, int totalCount, ParticleSlice[] slices) {
		return new ComputeResult(buffer, totalCount, slices, null, 0, 0);
	}

	public static ComputeResult ofIndirect(GpuBuffer buffer, int totalCount, ParticleSlice[] slices,
	                                       GpuBuffer indirectBuffer, int layerCount) {
		return ofIndirect(buffer, totalCount, slices, indirectBuffer, layerCount, 20);
	}

	public static ComputeResult ofIndirect(GpuBuffer buffer, int totalCount, ParticleSlice[] slices,
	                                       GpuBuffer indirectBuffer, int layerCount, int indirectCommandStride) {
		return new ComputeResult(buffer, totalCount, slices, indirectBuffer, layerCount, indirectCommandStride);
	}

	public int totalVertexCount() {
		return totalCount * 4;
	}

	public int totalIndexCount() {
		return totalCount * 6;
	}

	public boolean isIndirect() {
		return indirectBuffer != null;
	}

	public record ParticleSlice(SingleQuadParticle.Layer layer, int baseCount, int count) {
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
