package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.platform.GlStateManager;

import java.util.List;

public class ParticleVertexFormat {
	ParticleVertexFormatElement[] elements;
	int[] offsets;

	public ParticleVertexFormat(ParticleVertexFormatElement... elements) {
		this.offsets = new int[elements.length];

		int offset = 0;
		for (int i = 0, l = elements.length; i < l; ++i) {
			this.offsets[i] = offset;
			offset += elements[i].size();
		}

		this.elements = elements;
	}

	public void setupBufferState() {
		int vertexSize = this.getVertexSize();

		for (int i = 0, l = elements.length; i < l; ++i) {
			ParticleVertexFormatElement element = elements[i];
			GlStateManager._enableVertexAttribArray(i);
			element.setupBufferState(i, offsets[i], vertexSize);
		}
	}

	private int getVertexSize() {
		return 0;
	}
}
