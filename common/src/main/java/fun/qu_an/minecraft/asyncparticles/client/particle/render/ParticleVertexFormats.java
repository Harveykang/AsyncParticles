package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class ParticleVertexFormats {
	public static final int RAW_PARTICLE_BYTES = 68; // GPU_PARTICLE.getVertexSize();
	public static final int PROCESSED_PARTICLE_VERTEX_BYTES = 44; // PARTICLE.getVertexSize();
	public static final ParticleVertexFormatElement POSITION = new ParticleVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
	public static final ParticleVertexFormatElement COLOR = new ParticleVertexFormatElement(0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
	public static final ParticleVertexFormatElement UV0_4F = new ParticleVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4);
	public static final ParticleVertexFormatElement UV2_2I = new ParticleVertexFormatElement(2, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 2);
	public static final ParticleVertexFormatElement UV2_2S = new ParticleVertexFormatElement(2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
	public static final ParticleVertexFormatElement VEC2F = new ParticleVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
	public static final ParticleVertexFormatElement VEC3F = new ParticleVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 3);
	public static final ParticleVertexFormatElement VEC4F = new ParticleVertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
	public static final ParticleVertexFormatElement PADDING_4_BYTES = new ParticleVertexFormatElement(0, VertexFormatElement.Type.INT, null, 1);
	public static final ParticleVertexFormat RAW_PARTICLE = new ParticleVertexFormat(
		POSITION, // xo, yo, zo (vec3f)
		POSITION, // x, y, z (vec3f)
		VEC2F, // oSize, size
		UV0_4F, // u0, v0, u1, v1
		COLOR, COLOR, // color (vec2i)
		UV2_2S, // light
		VEC2F); // oRoll, roll
	public static final ParticleVertexFormat PROCESSED_PARTICLE = new ParticleVertexFormat(
		VEC3F, VEC2F, VEC4F, UV2_2I);
}
