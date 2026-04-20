package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.jetbrains.annotations.Nullable;

public record ParticleVertexFormatElement(int uv,
                                          VertexFormatElement.Type type,
                                          @Nullable VertexFormatElement.Usage usage,
                                          int count) {
	public void setupBufferState(int stride, int offset, int stateIndex) {
		if (usage == null) {
			return;
		}
		usage.setupState.setupBufferState(count, type.glType(), stride, offset, stateIndex);
	}

	public int size() {
		return type.size() * this.count;
	}
}
