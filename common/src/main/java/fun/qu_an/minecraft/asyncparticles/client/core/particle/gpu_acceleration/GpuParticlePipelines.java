package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ParticleVertexBuffer;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import org.lwjgl.opengl.ARBVertexAttribBinding;

import java.util.Map;

public class GpuParticlePipelines {
	public static final GpuFormat POSITION_FORMAT = GpuFormat.RGB32_FLOAT;
	public static final GpuFormat COLOR_FORMAT = GpuFormat.RGBA8_UNORM;
	public static final GpuFormat UV0_FORMAT = GpuFormat.RG32_FLOAT;
	public static final GpuFormat UV2_FORMAT = GpuFormat.RG16_SINT;
	public static final GpuFormat IDENTITY_COLOR_FORMAT = GpuFormat.RGBA32_FLOAT;
	public static final GpuFormat IDENTITY_UV2_FORMAT = GpuFormat.RG32_SINT;
	public static final VertexFormat RAW_PARTICLE = VertexFormat.builder(0)
		.addAttribute("oPosition", POSITION_FORMAT)
		.addAttribute("Position", POSITION_FORMAT)
		.addAttribute("Sizes", UV0_FORMAT)
		.addAttribute("oUV0", UV0_FORMAT)
		.addAttribute("UV0", UV0_FORMAT)
		.addAttribute("oColor", COLOR_FORMAT)
		.addAttribute("Color", COLOR_FORMAT)
		.addAttribute("Light", UV2_FORMAT)
		.addAttribute("Rolls", UV0_FORMAT)
		.build();
	public static final int[] multiDrawFirst = {0, 0};
	public static final int[] multiDrawCount = {0, 0};
	public static final VertexFormat IDENTITY_PARTICLE = VertexFormat.builder(0)
		.addAttribute("Position", POSITION_FORMAT) // 3 floats
		.addAttribute("UV0", UV0_FORMAT) // 2 floats
		.addAttribute("Color", IDENTITY_COLOR_FORMAT) // 4 floats
		.addAttribute("UV2", IDENTITY_UV2_FORMAT) // 2 ints
		.build();

	private static final Map<RenderPipeline, RenderPipeline> pipelines = new Reference2ReferenceOpenHashMap<>();

	public static RenderPipeline of(RenderPipeline original, boolean translucent) {
		if (original.getVertexFormatBinding(0) != DefaultVertexFormat.PARTICLE) {
			throw new IllegalArgumentException("Invalid vertex format");
		}
		return pipelines.computeIfAbsent(original, original1 -> {
			VertexFormat[] vertexFormats = original1.getVertexFormatBindings().clone();
			vertexFormats[0] = IDENTITY_PARTICLE;
			RenderPipeline pipeline = new RenderPipeline(
				original1.getLocation(),
				original1.getVertexShader(),
				original1.getFragmentShader(),
				original1.getShaderDefines(),
				original1.getBindGroupLayouts(),
				original1.getColorTargetStates(),
				original1.getDepthStencilState(),
				original1.getPolygonMode(),
				original1.isCull(),
				vertexFormats,
				original1.getPrimitiveTopology(),
				original1.getSortKey());
			if (ModListHelper.IRIS_LOADED) {
				IrisApi.getInstance().assignPipeline(pipeline,
					translucent ? IrisProgram.PARTICLES_TRANSLUCENT : IrisProgram.PARTICLES);
			}
			return pipeline;
		});
	}

	public static void glBindAttr(VertexFormat format, ParticleVertexBuffer buffer) {
		buffer.bind();
		if (BackendCaps.GL_ARB_vertex_attrib_binding) {
			int attribLocation = 0;
			for (VertexFormatElement element : format.getElements()) {
				GlStateManager._enableVertexAttribArray(attribLocation);
				int glExternalId = GlConst.toGlExternalId(element.format());
				int glType = GlConst.toGlType(element.format());
				boolean isIntegerFormat = GlConst.isGlFormatInteger(glExternalId);
				boolean isNormalizedFormat = GlConst.isFormatNormalized(element.format());
				int channelCount = GlConst.glFormatChannelCount(glExternalId);
				if (isIntegerFormat) {
					ARBVertexAttribBinding.glVertexAttribIFormat(attribLocation, channelCount, glType, element.offset());
				} else {
					ARBVertexAttribBinding.glVertexAttribFormat(attribLocation, channelCount, glType, isNormalizedFormat, element.offset());
				}

				ARBVertexAttribBinding.glVertexAttribBinding(attribLocation, 0);
				attribLocation++;
			}
			ARBVertexAttribBinding.glBindVertexBuffer(0, buffer.vbo, 0L, format.getVertexSize());
//			ARBVertexAttribBinding.glVertexBindingDivisor(0, format.getStepRate());
		} else {
			int vertexSize = format.getVertexSize();
			int attributeIndex = 0;
			for (VertexFormatElement element : format.getElements()) {
				int glExternalId = GlConst.toGlExternalId(element.format());
				int glType = GlConst.toGlType(element.format());
				boolean isIntegerFormat = GlConst.isGlFormatInteger(glExternalId);
				boolean isNormalizedFormat = GlConst.isFormatNormalized(element.format());
				int channelCount = GlConst.glFormatChannelCount(glExternalId);
				GlStateManager._enableVertexAttribArray(attributeIndex);

				if (isIntegerFormat) {
					GlStateManager._vertexAttribIPointer(attributeIndex, channelCount, glType, vertexSize, element.offset());
				} else {
					GlStateManager._vertexAttribPointer(attributeIndex, channelCount, glType, isNormalizedFormat, vertexSize, element.offset());
				}

//				GL33C.glVertexAttribDivisor(attributeIndex, format.getStepRate());
				attributeIndex++;
			}
		}
		ParticleVertexBuffer.unbind();
	}
}
