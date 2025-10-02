package fun.qu_an.minecraft.asyncparticles.client.particle.shader;

import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;

public class ParticleComputeShader {
	public static final ParticleComputeShader INSTANCE = new ParticleComputeShader();
	public static final int INPUT_BINDING = 0;
	public static final int OUTPUT_BINDING = 1;
	public static final int COUNTER_BINDING = 2;
	public final int programId;
	public final int partialTicksLocation;
	public final int cameraLeftLocation;
	public final int cameraUpLocation;
	public final int cameraDeltaLocation;
	public final int particleCountLocation;
	public final int overflowCountLocation;
	public final int[] frustumPlaneLocations = new int[6]; // vec4[6]

	public ParticleComputeShader() {
		programId = ShaderCompiler.createShaderProgram(GL43C.GL_COMPUTE_SHADER,
			"/assets/asyncparticles/particle_gpu_acceleration/particle_cs.comp");

		// Uniform locations
		partialTicksLocation = GL20C.glGetUniformLocation(programId, "uPartialTicks");
		cameraLeftLocation = GL20C.glGetUniformLocation(programId, "uCameraLeft");
		cameraUpLocation = GL20C.glGetUniformLocation(programId, "uCameraUp");
		cameraDeltaLocation = GL20C.glGetUniformLocation(programId, "uCameraDelta");
		particleCountLocation = GL20C.glGetUniformLocation(programId, "uParticleCount");
		overflowCountLocation = GL20C.glGetUniformLocation(programId, "uOverflowCount");
		for (int i = 0; i < 6; i++) {
			frustumPlaneLocations[i] = GL20C.glGetUniformLocation(programId, "uFrustumPlanes[" + i + "]");
		}
	}

	public void use() {
		GL20C.glUseProgram(programId);
	}

	public void setup(
		float partialTicks,
		float lx, float ly, float lz,
		float ux, float uy, float uz,
		float dx, float dy, float dz,
		int particleCount,
		int overflowCount,
		Vector4f[] frustumPlanes // 6 planes: left, right, bottom, top, near, far
	) {
		if (frustumPlanes.length != 6) {
			throw new IllegalArgumentException("Frustum planes array must have 6 elements");
		}
		GL20C.glUniform1f(partialTicksLocation, partialTicks);
		GL20C.glUniform3f(cameraLeftLocation, lx, ly, lz);
		GL20C.glUniform3f(cameraUpLocation, ux, uy, uz);
		GL20C.glUniform3f(cameraDeltaLocation, dx, dy, dz);
		GL20C.glUniform1i(particleCountLocation, particleCount);
		GL20C.glUniform1i(overflowCountLocation, overflowCount);

		// 上传 6 个 frustum planes (each is vec4: ax + by + cz + d >= 0)
		for (int i = 0, l = frustumPlaneLocations.length; i < l; i++) {
			int location = frustumPlaneLocations[i];
			Vector4f p = frustumPlanes[i];
			GL20C.glUniform4f(location, p.x, p.y, p.z, p.w);
		}
	}

	/**
	 * 绑定输入和输出 SSBO
	 * - inputBinding: 对应 GLSL 中 layout(binding = N) readonly buffer Input { ... };
	 * - outputBinding: 对应 layout(binding = M) writeonly buffer Output { ... };
	 */
	public static void bindBuffers(
		int inputSSBO,
		int outputSSBO,
		int counterBuffer
	) {
		GLCaps.csSupport.glBindShaderStorageBufferBase(INPUT_BINDING, inputSSBO);
		GLCaps.csSupport.glBindShaderStorageBufferBase(OUTPUT_BINDING, outputSSBO);
		GLCaps.csSupport.glBindShaderStorageBufferBase(COUNTER_BINDING, counterBuffer);
	}
}
