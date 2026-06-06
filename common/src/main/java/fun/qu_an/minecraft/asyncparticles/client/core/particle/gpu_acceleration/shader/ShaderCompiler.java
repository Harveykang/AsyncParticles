package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.shader;

import com.mojang.blaze3d.opengl.GlConst;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20C;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntConsumer;

public class ShaderCompiler {
	public static int createShaderProgram(int type, String resource) {
		return createShaderProgram(type, resource, _ -> {});
	}

	public static int createShaderProgram(int type, String resource, IntConsumer beforeLink) {
		final int programId;
		String source;
		try (InputStream is = ShaderCompiler.class
			.getResourceAsStream(resource)) {
			source = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load shader '" + resource + "'", e);
		}

		int shaderId = GL20C.glCreateShader(type);
		GL20C.glShaderSource(shaderId, source);
		GL20C.glCompileShader(shaderId);

		int compileStatus = GL20C.glGetShaderi(shaderId, GlConst.GL_COMPILE_STATUS);
		if (compileStatus == GlConst.GL_FALSE) {
			String log = GL20C.glGetShaderInfoLog(shaderId);
			GL20C.glDeleteShader(shaderId);
			throw new RuntimeException("Compile error: " + log);
		}

		programId = GL20C.glCreateProgram();
		GL20C.glAttachShader(programId, shaderId);
		beforeLink.accept(programId);
		GL20C.glLinkProgram(programId);
		GL20C.glDeleteShader(shaderId);

		int linkStatus = GL20C.glGetProgrami(programId, GlConst.GL_LINK_STATUS);
		if (linkStatus == GlConst.GL_FALSE) {
			String log = GL20C.glGetProgramInfoLog(programId, 1024);
			throw new RuntimeException("Link error: " + log);
		}
		return programId;
	}
}
