package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.jetbrains.annotations.Nullable;

public record ParticleVertexFormatElement(int uv, // index
                                          VertexFormatElement.Type type,
                                          @Nullable VertexFormatElement.Usage usage,
                                          int count) {
	public void setupBufferState(int stride, int offset, int stateIndex) {
		if (usage == null) {
			return;
		}
		usage.setupBufferState(this.count, this.type.getGlType(), stride, offset, uv, stateIndex);
	}

	public int size() {
		return type.getSize() * this.count;
	}
}
