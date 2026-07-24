package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import com.mojang.blaze3d.opengl.GlConst;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
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
			"/assets/asyncparticles/particle_gpu_acceleration/gl_particle_transformfeedback.vert",
			programId -> {
				// same as GpuParticlePipelines.RAW_PARTICLE
				GL20C.glBindAttribLocation(programId, 0, "oPosition");
				GL20C.glBindAttribLocation(programId, 1, "Position");
				GL20C.glBindAttribLocation(programId, 2, "Sizes");
				GL20C.glBindAttribLocation(programId, 3, "UVMin");
				GL20C.glBindAttribLocation(programId, 4, "UVMax");
				GL20C.glBindAttribLocation(programId, 5, "oColor");
				GL20C.glBindAttribLocation(programId, 6, "Color");
				GL20C.glBindAttribLocation(programId, 7, "Light");
				GL20C.glBindAttribLocation(programId, 8, "Rolls");
				Backends.glTf.glTransformFeedbackVaryings(programId,
					new String[]{
						"Position_0", "UV0_0", "Color_0", "UV2_0",
						"Position_1", "UV0_1", "Color_1", "UV2_1",
						"Position_2", "UV0_2", "Color_2", "UV2_2",
						"Position_3", "UV0_3", "Color_3", "UV2_3"
					},
					GL30C.GL_INTERLEAVED_ATTRIBS);
			}
		);

		int loc = GL20C.glGetAttribLocation(programId, "Rolls");
		if (loc != 8) {
			throw new IllegalStateException("Attrib binding failed: Rolls at " + loc);
		}

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
