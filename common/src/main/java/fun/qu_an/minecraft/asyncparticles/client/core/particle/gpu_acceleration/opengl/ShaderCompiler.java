package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import com.mojang.blaze3d.platform.GlConst;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20C;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntConsumer;

public class ShaderCompiler {
	public static int createShaderProgram(int type, String resource) {
		return createShaderProgram(type, resource, ignored -> {
		});
	}

	public static int createShaderProgram(int type, String resource, IntConsumer beforeLink) {
		String source;
		try (InputStream is = ShaderCompiler.class
			.getResourceAsStream(resource)) {
			source = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load shader '" + resource + "'", e);
		}

		int shaderId = GL20C.glCreateShader(type);
		int programId = 0;
		try {
			GL20C.glShaderSource(shaderId, source);
			GL20C.glCompileShader(shaderId);

			int compileStatus = GL20C.glGetShaderi(shaderId, GlConst.GL_COMPILE_STATUS);
			if (compileStatus == GlConst.GL_FALSE) {
				String log = GL20C.glGetShaderInfoLog(shaderId);
				throw new IllegalStateException("Compile error: " + log);
			}

			programId = GL20C.glCreateProgram();
			GL20C.glAttachShader(programId, shaderId);
			beforeLink.accept(programId);
			GL20C.glLinkProgram(programId);

			int linkStatus = GL20C.glGetProgrami(programId, GlConst.GL_LINK_STATUS);
			if (linkStatus == GlConst.GL_FALSE) {
				String log = GL20C.glGetProgramInfoLog(programId, 1024);
				throw new IllegalStateException("Link error: " + log);
			}
			return programId;
		} catch (RuntimeException | Error exception) {
			if (programId != 0) {
				GL20C.glDeleteProgram(programId);
			}
			throw exception;
		} finally {
			GL20C.glDeleteShader(shaderId);
		}
	}
}
