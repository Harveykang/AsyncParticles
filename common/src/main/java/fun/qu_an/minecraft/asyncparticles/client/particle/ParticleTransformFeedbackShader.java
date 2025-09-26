package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.platform.GlConst;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ParticleTransformFeedbackShader {
	public static final ParticleTransformFeedbackShader TF_SHADER = new ParticleTransformFeedbackShader();
	public final int programId;
	// Uniforms
	public final int PartialTick;
	public final int CameraLeft;
	public final int CameraUp;
	public final int PartialCameraPos;

	protected ParticleTransformFeedbackShader() {
		// Shader
		String source;
		try (InputStream is = ParticleTransformFeedbackShader.class.getResourceAsStream("/assets/asyncparticles/particle_gpu_acceleration/particle_tf.vsh")) {
			source = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		int shaderId = GL20.glCreateShader(GlConst.GL_VERTEX_SHADER);
		GL20C.glShaderSource(shaderId, source);
		GL20.glCompileShader(shaderId);
		// Check
		int compileStatus = GL20C.glGetShaderi(shaderId, GlConst.GL_COMPILE_STATUS);
		if (compileStatus == GlConst.GL_FALSE) {
			String log = GL20C.glGetShaderInfoLog(shaderId);
			GL20C.glDeleteShader(shaderId);
			throw new RuntimeException("Vertex Shader compilation failed: " + log);
		}

		programId = GL20C.glCreateProgram();
		GL20C.glAttachShader(programId, shaderId);
		GLCaps.tfSupport.glTransformFeedbackVaryings(programId,
			new String[]{
				"Position_0", "UV0_0", "Color_0", "UV2_0",
				"Position_1", "UV0_1", "Color_1", "UV2_1",
				"Position_2", "UV0_2", "Color_2", "UV2_2",
				"Position_3", "UV0_3", "Color_3", "UV2_3"
			},
			GL30C.GL_INTERLEAVED_ATTRIBS);
		GL20C.glLinkProgram(programId);
		// Check
		int linkStatus = GL20C.glGetProgrami(programId, GlConst.GL_LINK_STATUS);
		if (linkStatus == GlConst.GL_FALSE) {
			String log = GL20C.glGetShaderInfoLog(shaderId);
			throw new RuntimeException("Shader Program linking failed: " + log);
		}
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.linkUniformBlock(tshProg);
//		}

		GL20C.glDeleteShader(shaderId);

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
