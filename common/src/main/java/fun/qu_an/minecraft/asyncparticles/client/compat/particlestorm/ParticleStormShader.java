package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ShaderCompiler;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public final class ParticleStormShader {
	public static final ParticleStormShader INSTANCE = new ParticleStormShader();
	private final Shader normalShader;
	private final Shader cameraRotationShader;

	private record Shader(int program, int partialTick, int cameraOffset,
						  int cameraRotation, int cameraLeft, int cameraUp) {
	}

	private ParticleStormShader() {
		cameraRotationShader = createShader("facing_screen_quad.vert");
		try {
			normalShader = createShader("common_quad.vert");
		} catch (RuntimeException | Error exception) {
			GL20C.glDeleteProgram(cameraRotationShader.program());
			throw exception;
		}
	}

	private static Shader createShader(String file) {
		int program = ShaderCompiler.createShaderProgram(GL20C.GL_VERTEX_SHADER,
			"/assets/asyncparticles/compat/particlestorm/" + file, id -> {
				String[] attributes = {"OldPosition", "Position", "Size", "UVMin", "UVMax", "Color", "Light",
					"OldRotation", "RotationAndMode", "Facing"};
				for (int i = 0; i < attributes.length; i++) {
					GL20C.glBindAttribLocation(id, i, attributes[i]);
				}
				Backends.glTf.glTransformFeedbackVaryings(id, new String[]{
					"Position_0", "UV0_0", "Color_0", "UV2_0",
					"Position_1", "UV0_1", "Color_1", "UV2_1",
					"Position_2", "UV0_2", "Color_2", "UV2_2",
					"Position_3", "UV0_3", "Color_3", "UV2_3"
				}, GL30C.GL_INTERLEAVED_ATTRIBS);
			});
		try {
			return new Shader(program,
				GL20C.glGetUniformLocation(program, "PartialTick"),
				GL20C.glGetUniformLocation(program, "CameraOffset"),
				GL20C.glGetUniformLocation(program, "CameraRotation"),
				GL20C.glGetUniformLocation(program, "CameraLeft"),
				GL20C.glGetUniformLocation(program, "CameraUp"));
		} catch (RuntimeException | Error exception) {
			GL20C.glDeleteProgram(program);
			throw exception;
		}
	}

	public void setup(boolean cameraOnly, Camera camera, float partialTicks, Vec3 snapshotCameraPos) {
		Shader selected = cameraOnly ? cameraRotationShader : normalShader;
		GL20C.glUseProgram(selected.program());
		GL20C.glUniform1f(selected.partialTick(), partialTicks);
		// Apply the shared surface offset once per frame, rather than once per particle.
		GL20C.glUniform3f(selected.cameraOffset(),
			(float) (snapshotCameraPos.x - camera.getPosition().x),
			(float) (snapshotCameraPos.y - camera.getPosition().y) + 1.0e-4f,
			(float) (snapshotCameraPos.z - camera.getPosition().z));
		if (!cameraOnly) {
			Quaternionf rotation = camera.rotation();
			GL20C.glUniform4f(selected.cameraRotation(), rotation.x, rotation.y, rotation.z, rotation.w);
		}
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		GL20C.glUniform3f(selected.cameraLeft(), left.x, left.y, left.z);
		GL20C.glUniform3f(selected.cameraUp(), up.x, up.y, up.z);
	}
}
