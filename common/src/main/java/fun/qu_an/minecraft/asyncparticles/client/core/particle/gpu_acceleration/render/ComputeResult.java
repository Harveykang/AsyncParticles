package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.particle.SingleQuadParticle;

public record ComputeResult(GpuBuffer buffer, int totalCount, ParticleSlice[] slices) {
	public int totalVertexCount() {
		return totalCount * 4;
	}

	public int totalIndexCount() {
		return totalCount * 6;
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
	}
}
