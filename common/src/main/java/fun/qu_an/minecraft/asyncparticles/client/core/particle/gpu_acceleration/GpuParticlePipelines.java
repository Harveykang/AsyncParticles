package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class GpuParticlePipelines {
	public static final VertexFormatElement PLAIN_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.FLOAT, false, 4);
	public static final VertexFormatElement PLAIN_UV2 = new VertexFormatElement(4, 0, VertexFormatElement.Type.INT, false, 2);
	public static final VertexFormat PLAIN_PARTICLE;
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
	public static final int[] multiDrawIndex = {0, 0};
	public static final int[] multiDrawCount = {0, 0};

	static {
		VertexFormat.Builder add = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION) // 3 floats
			.add("UV0", VertexFormatElement.UV0) // 2 floats
			.add("Color", PLAIN_COLOR) // 4 floats
			.add("UV2", PLAIN_UV2); // 2 shorts
		VertexFormat format = add.build();
		PLAIN_PARTICLE = new CustomVertexFormat(format.getElements(), format.getElementAttributeNames(), add.offsets, format.getVertexSize());
	}
	private static final Map<RenderPipeline, RenderPipeline> pipelines = new Reference2ReferenceOpenHashMap<>();

	public static RenderPipeline of(RenderPipeline original) {
		if (original.getVertexFormat() != DefaultVertexFormat.PARTICLE) {
			throw new IllegalArgumentException("Invalid vertex format");
		}
		return pipelines.computeIfAbsent(original, _ -> new RenderPipeline(original.getLocation(),
			original.getVertexShader(),
			original.getFragmentShader(),
			original.getShaderDefines(),
			original.getSamplers(),
			original.getUniforms(),
			original.getColorTargetState(),
			original.getDepthStencilState(),
			original.getPolygonMode(),
			original.isCull(),
			PLAIN_PARTICLE,
			original.getVertexFormatMode(),
			original.getSortKey()));
	}

	private static class CustomVertexFormat extends VertexFormat {
		private final int plainColorOffset;
		private final int plainUv2Offset;
		private CustomVertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
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
