package fun.qu_an.minecraft.asyncparticles.client.particle.shader;

import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;

public class ParticleCounterToIndirectShader {
	public static final ParticleCounterToIndirectShader INSTANCE = new ParticleCounterToIndirectShader();
	public final int programId;

	public ParticleCounterToIndirectShader() {
		programId = ShaderCompiler.createShaderProgram(GL43C.GL_COMPUTE_SHADER,
			"/assets/asyncparticles/particle_gpu_acceleration/counter_to_indirect.comp");
	}

	public void use() {
		GL20C.glUseProgram(programId);
	}

	public static void bindBuffers(int counterBuffer, int indirectBuffer) {
		GLCaps.csSupport.glBindShaderStorageBufferBase(0, counterBuffer);
		GLCaps.csSupport.glBindShaderStorageBufferBase(1, indirectBuffer);
	}
}
