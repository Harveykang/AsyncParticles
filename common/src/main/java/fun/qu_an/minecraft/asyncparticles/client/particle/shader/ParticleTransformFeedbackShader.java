package fun.qu_an.minecraft.asyncparticles.client.particle.shader;

import com.mojang.blaze3d.platform.GlConst;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public class ParticleTransformFeedbackShader {
	public static final ParticleTransformFeedbackShader INSTANCE = new ParticleTransformFeedbackShader();
	public final int programId;
	// Uniforms
	public final int PartialTick;
	public final int CameraLeft;
	public final int CameraUp;
	public final int PartialCameraPos;

	protected ParticleTransformFeedbackShader() {
		programId = ShaderCompiler.createShaderProgram(
			GlConst.GL_VERTEX_SHADER,
			"/assets/asyncparticles/particle_gpu_acceleration/particle_tf.vert",
			programId -> GLCaps.tfSupport.glTransformFeedbackVaryings(programId,
				new String[]{
					"Position_0", "UV0_0", "Color_0", "UV2_0",
					"Position_1", "UV0_1", "Color_1", "UV2_1",
					"Position_2", "UV0_2", "Color_2", "UV2_2",
					"Position_3", "UV0_3", "Color_3", "UV2_3"
				},
				GL30C.GL_INTERLEAVED_ATTRIBS)
		);

		// Uniforms
		PartialTick = GL20C.glGetUniformLocation(programId, "PartialTick");
		CameraLeft = GL20C.glGetUniformLocation(programId, "CameraLeft");
		CameraUp = GL20C.glGetUniformLocation(programId, "CameraUp");
		PartialCameraPos = GL20C.glGetUniformLocation(programId, "PartialCameraPos");
	}

	public void use() {
		GL30C.glUseProgram(programId);
	}

	public void setup(float partialTicks,
					  float lvX,
					  float lvY,
					  float lvZ,
					  float upX,
					  float upY,
					  float upZ,
					  float partialCameraX,
					  float partialCameraY,
					  float partialCameraZ) {
		GL20C.glUniform1f(PartialTick, partialTicks);
		GL20C.glUniform3f(CameraLeft, lvX, lvY, lvZ);
		GL20C.glUniform3f(CameraUp, upX, upY, upZ);
		GL20C.glUniform3f(PartialCameraPos, partialCameraX, partialCameraY, partialCameraZ);
	}
}
