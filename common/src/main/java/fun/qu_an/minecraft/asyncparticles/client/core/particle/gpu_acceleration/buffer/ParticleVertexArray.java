package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer;

import org.lwjgl.opengl.GL30C;

public class ParticleVertexArray {
	public final int handle;

	public ParticleVertexArray() {
		handle = GL30C.glGenVertexArrays();
	}

	public void bind() {
		GL30C.glBindVertexArray(handle);
	}

	public void unbind() {
		GL30C.glBindVertexArray(0);
	}

	public void delete() {
		GL30C.glDeleteVertexArrays(handle);
	}
}
