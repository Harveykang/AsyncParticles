package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.platform.GlStateManager;

import java.util.List;

public class ParticleVertexFormat {
	private final ParticleVertexFormatElement[] elements;
	private final int[] offsets;
	public final int size;

	public ParticleVertexFormat(ParticleVertexFormatElement... elements) {
		this.offsets = new int[elements.length];

		int offset = 0;
		for (int i = 0, l = elements.length; i < l; ++i) {
			this.offsets[i] = offset;
			offset += elements[i].size();
		}

		this.elements = elements;
		size = offset;
	}

	public void setupBufferState() {
		for (int i = 0, l = elements.length; i < l; ++i) {
			ParticleVertexFormatElement element = elements[i];
			GlStateManager._enableVertexAttribArray(i);
			element.setupBufferState(size, offsets[i], i);
		}
	}
}
