package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
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
	public static final VertexFormatElement PLAIN_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.FLOAT, false, 4);
	public static final VertexFormatElement PLAIN_UV2 = new VertexFormatElement(4, 0, VertexFormatElement.Type.INT, false, 2);
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
	public static final VertexFormat PLAIN_PARTICLE;

	static {
		VertexFormat.Builder add = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION) // 3 floats
			.add("UV0", VertexFormatElement.UV0) // 2 floats
			.add("Color", PLAIN_COLOR) // 4 floats
			.add("UV2", PLAIN_UV2); // 2 ints
		VertexFormat format = add.build();
		PLAIN_PARTICLE = new PlainParticleVertexFormat(format.getElements(),
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
				PLAIN_PARTICLE,
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
		ParticleVertexBuffer.unbind();
	}

	private static class PlainParticleVertexFormat extends VertexFormat {
		private final int plainColorOffset;
		private final int plainUv2Offset;

		private PlainParticleVertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
			super(elements, names, offsets, vertexSize);
			plainColorOffset = offsets.getInt(elements.indexOf(PLAIN_COLOR));
			plainUv2Offset = offsets.getInt(elements.indexOf(PLAIN_UV2));
		}

		@Override
		public int getOffset(@NonNull VertexFormatElement element) {
			if (element == PLAIN_COLOR) {
				return plainColorOffset;
			}
			if (element == PLAIN_UV2) {
				return plainUv2Offset;
			}
			return super.getOffset(element);
		}
	}
}
