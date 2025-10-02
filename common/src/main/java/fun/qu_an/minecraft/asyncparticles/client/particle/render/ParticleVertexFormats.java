package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class ParticleVertexFormats {
	public static final int RAW_PARTICLE_BYTES = 68; // GPU_PARTICLE.getVertexSize();
	public static final int PROCESSED_PARTICLE_VERTEX_BYTES = 44; // PARTICLE.getVertexSize();
	public static final int COMPUTE_SHADER_RAW_PARTICLE_BYTES = 96; // COMPUTE_SHADER_PARTICLE.getVertexSize();
	public static final int COMPUTE_SHADER_PROCESSED_PARTICLE_VERTEX_BYTES = 64; // COMPUTE_SHADER_PROCESSED_PARTICLE.getVertexSize();
	public static final VertexFormatElement UV0_4F = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4);
	public static final VertexFormatElement UV2_2I = new VertexFormatElement(2, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement VEC2F = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
	public static final VertexFormatElement VEC3F = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3);
	public static final VertexFormatElement VEC4F = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
	public static final VertexFormatElement PADDING_4_BYTES = new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.PADDING, 1);
	public static final VertexFormat RAW_PARTICLE = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
		// xo, yo, zo
		.put("oPosition", DefaultVertexFormat.ELEMENT_POSITION)
		// x, y, z
		.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
		// oSize, size
		.put("Sizes", VEC2F)
		// u0, v0, u1, v1
		.put("UVMinMax", UV0_4F)
		// color
		.put("oColor", DefaultVertexFormat.ELEMENT_COLOR)
		.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
		// light
		.put("Light", DefaultVertexFormat.ELEMENT_UV2)
		// oRoll, roll
		.put("Rolls", VEC2F)
		.build());
	public static final VertexFormat COMPUTE_SHADER_PARTICLE = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
		// xo, yo, zo
		.put("oPosition", DefaultVertexFormat.ELEMENT_POSITION)
		// padding
		.put("Padding0", PADDING_4_BYTES)
		// x, y, z
		.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
		// padding
		.put("Padding1", PADDING_4_BYTES)
		// oSize, size
		.put("Sizes", VEC2F)
		// padding
		.put("Padding2", PADDING_4_BYTES)
		// padding
		.put("Padding3", PADDING_4_BYTES)
		// u0, v0, u1, v1
		.put("UVMinMax", UV0_4F)
		// color
		.put("oColor", DefaultVertexFormat.ELEMENT_COLOR)
		.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
		// light
		.put("Light", DefaultVertexFormat.ELEMENT_UV2)
		// padding
		.put("Padding4", PADDING_4_BYTES)
		// oRoll, roll
		.put("Rolls", VEC2F)
		// padding
		.put("Padding5", PADDING_4_BYTES)
		// padding
		.put("Padding6", PADDING_4_BYTES)
		.build());
	public static final VertexFormat PROCESSED_PARTICLE = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
		.put("Position", VEC3F)
		.put("UV0", VEC2F)
		.put("Color", VEC4F)
		.put("UV2", UV2_2I)
		.build());
	public static final VertexFormat COMPUTE_SHADER_PROCESSED_PARTICLE = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
		.put("Position", VEC3F)
		// padding
		.put("Padding0", PADDING_4_BYTES)
		.put("UV0", VEC2F)
		// padding
		.put("Padding1", PADDING_4_BYTES)
		// padding
		.put("Padding2", PADDING_4_BYTES)
		.put("Color", VEC4F)
		.put("UV2", UV2_2I)
		// padding
		.put("Padding3", PADDING_4_BYTES)
		// padding
		.put("Padding4", PADDING_4_BYTES)
		.build());
}
