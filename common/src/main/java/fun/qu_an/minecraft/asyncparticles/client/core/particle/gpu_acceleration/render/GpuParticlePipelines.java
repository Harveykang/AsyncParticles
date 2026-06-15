package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer.ParticleVertexBuffer;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.ARBVertexAttribBinding;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class GpuParticlePipelines {
	public static final VertexFormatElement IDENTITY_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.FLOAT, false, 4);
	public static final VertexFormatElement IDENTITY_UV2 = new VertexFormatElement(4, 0, VertexFormatElement.Type.INT, false, 2);
	public static final VertexFormat RAW_PARTICLE = VertexFormat.builder()
		.add("oPosition", VertexFormatElement.POSITION)
		.add("Position", VertexFormatElement.POSITION)
		.add("Sizes", VertexFormatElement.UV0)
		.add("oUV0", VertexFormatElement.UV0)
		.add("UV0", VertexFormatElement.UV0)
		.add("oColor", VertexFormatElement.COLOR)
		.add("Color", VertexFormatElement.COLOR)
		.add("Light", VertexFormatElement.UV2)
		.add("Rolls", VertexFormatElement.UV0)
		.build();
	public static final int[] multiDrawFirst = {0, 0};
	public static final int[] multiDrawCount = {0, 0};
	public static final VertexFormat IDENTITY_PARTICLE;

	static {
		VertexFormat.Builder add = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION) // 3 floats
			.add("UV0", VertexFormatElement.UV0) // 2 floats
			.add("Color", IDENTITY_COLOR) // 4 floats
			.add("UV2", IDENTITY_UV2); // 2 ints
		VertexFormat format = add.build();
		IDENTITY_PARTICLE = new IdentityParticleVertexFormat(format.getElements(),
			format.getElementAttributeNames(),
			add.offsets,
			format.getVertexSize());
	}

	private static final Map<RenderPipeline, RenderPipeline> pipelines = new Reference2ReferenceOpenHashMap<>();

	public static RenderPipeline of(RenderPipeline original, BooleanSupplier translucentSupplier) {
		if (original.getVertexFormat() != DefaultVertexFormat.PARTICLE) {
			throw new IllegalArgumentException("Invalid vertex format");
		}
		return pipelines.computeIfAbsent(original, original1 -> {
			RenderPipeline pipeline = new RenderPipeline(
				original1.getLocation(),
				original1.getVertexShader(),
				original1.getFragmentShader(),
				original1.getShaderDefines(),
				original1.getSamplers(),
				original1.getUniforms(),
				original1.getColorTargetState(),
				original1.getDepthStencilState(),
				original1.getPolygonMode(),
				original1.isCull(),
				IDENTITY_PARTICLE,
				original1.getVertexFormatMode(),
				original1.getSortKey());
			if (ModListHelper.IRIS_LOADED) {
				IrisApi.getInstance().assignPipeline(pipeline,
					translucentSupplier.getAsBoolean() ? IrisProgram.PARTICLES_TRANSLUCENT : IrisProgram.PARTICLES);
			}
			return pipeline;
		});
	}

	public static void bindAttr(VertexFormat format, ParticleVertexBuffer buffer) {
		buffer.bind();
		if (GLCaps.supportsARBVertexAttribBinding) {
			List<VertexFormatElement> elements = format.getElements();
			for (int i = 0, offset = 0; i < elements.size(); i++) {
				VertexFormatElement element = elements.get(i);
				GlStateManager._enableVertexAttribArray(i);
				if (!element.normalized() && element.type() != VertexFormatElement.Type.FLOAT) {
					ARBVertexAttribBinding.glVertexAttribIFormat(i, element.count(), GlConst.toGl(element.type()), offset);
				} else {
					ARBVertexAttribBinding.glVertexAttribFormat(i, element.count(), GlConst.toGl(element.type()), element.normalized(), offset);
				}
				ARBVertexAttribBinding.glVertexAttribBinding(i, 0);
				offset += element.byteSize();
			}
			ARBVertexAttribBinding.glBindVertexBuffer(0, buffer.vbo, 0L, format.getVertexSize());
		} else {
			int vertexSize = format.getVertexSize();
			List<VertexFormatElement> elements = format.getElements();
			for (int i = 0, offset = 0; i < elements.size(); i++) {
				VertexFormatElement element = elements.get(i);
				GlStateManager._enableVertexAttribArray(i);

				if (!element.normalized() && element.type() != VertexFormatElement.Type.FLOAT) {
					GlStateManager._vertexAttribIPointer(i, element.count(), GlConst.toGl(element.type()), vertexSize, offset);
				} else {
					GlStateManager._vertexAttribPointer(i, element.count(), GlConst.toGl(element.type()), element.normalized(), vertexSize, offset);
				}
				offset += element.byteSize();
			}
		}
		ParticleVertexBuffer.unbind();
	}

	private static class IdentityParticleVertexFormat extends VertexFormat {
		private final int identityColorOffset;
		private final int identityUv2Offset;

		private IdentityParticleVertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
			super(elements, names, offsets, vertexSize);
			identityColorOffset = offsets.getInt(elements.indexOf(IDENTITY_COLOR));
			identityUv2Offset = offsets.getInt(elements.indexOf(IDENTITY_UV2));
		}

		@Override
		public int getOffset(@NonNull VertexFormatElement element) {
			if (element == IDENTITY_COLOR) {
				return identityColorOffset;
			}
			if (element == IDENTITY_UV2) {
				return identityUv2Offset;
			}
			return super.getOffset(element);
		}
	}
}
