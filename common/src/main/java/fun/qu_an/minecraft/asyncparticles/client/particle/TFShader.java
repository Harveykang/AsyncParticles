package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.platform.GlConst;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TFShader {
	public static final TFShader TF_SHADER = new TFShader();
	public final int tshProg;
	// Uniforms
	public final int PartialTick;
	public final int CameraLeft;
	public final int CameraUp;
	public final int PartialCameraPos;

	protected TFShader() {
		// Shader
		int tsh = GL20.glCreateShader(GlConst.GL_VERTEX_SHADER);
		try (InputStream is = TFShader.class.getResourceAsStream("/assets/asyncparticles/transform_feedback/particle_tf.vsh")) {
			GL20C.glShaderSource(tsh, IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		GL20.glCompileShader(tsh);
		// Check
		int compileStatus = GL20C.glGetShaderi(tsh, GlConst.GL_COMPILE_STATUS);
		if (compileStatus == GlConst.GL_FALSE) {
			String infoLog = GL20C.glGetShaderInfoLog(tsh, 1024); // 获取足够长的日志
			throw new RuntimeException("Vertex Shader compilation failed: " + infoLog);
		}

		tshProg = GL20C.glCreateProgram();
		GL20C.glAttachShader(tshProg, tsh);
		GL30C.glTransformFeedbackVaryings(tshProg,
			new String[]{
				"Position_0", "UV0_0", "Color_0", "UV2_0",
				"Position_1", "UV0_1", "Color_1", "UV2_1",
				"Position_2", "UV0_2", "Color_2", "UV2_2",
				"Position_3", "UV0_3", "Color_3", "UV2_3"
			},
			GL30C.GL_INTERLEAVED_ATTRIBS);
		GL20C.glLinkProgram(tshProg);
		// Check
		int linkStatus = GL20C.glGetProgrami(tshProg, GlConst.GL_LINK_STATUS);
		if (linkStatus == GlConst.GL_FALSE) {
			String infoLog = GL20C.glGetProgramInfoLog(tshProg, 1024);
			throw new RuntimeException("Shader Program linking failed: " + infoLog);
		}
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.linkUniformBlock(tshProg);
//		}

		GL20C.glDeleteShader(tsh);

		// Uniforms
		PartialTick = GL20C.glGetUniformLocation(tshProg, "PartialTick");
		CameraLeft = GL20C.glGetUniformLocation(tshProg, "CameraLeft");
		CameraUp = GL20C.glGetUniformLocation(tshProg, "CameraUp");
		PartialCameraPos = GL20C.glGetUniformLocation(tshProg, "PartialCameraPos");
	}

	public void use() {
		GL30C.glUseProgram(tshProg);
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
