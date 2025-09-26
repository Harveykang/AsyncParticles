package fun.qu_an.minecraft.asyncparticles.client.particle;

import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.*;

// TODO: DSA
public class ParticleVertexBuffer {
	public final int vao;
	public final int vbo;
	private ByteBuffer oldBuffer;
	int size;
	private boolean mapped = false;

	public ParticleVertexBuffer() {
		this.vao = glGenVertexArrays();
		this.vbo = glGenBuffers();
	}

	public static void unbind() {
		GL30C.glBindVertexArray(0);
		GL15C.glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	public void bind() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
	}

	public boolean resize(int size) {
		if (this.size >= size) {
			return false;
		}
		resize0(size);
		return true;
	}

	public void resize0(int size) {
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, size, GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		this.size = size;
	}

	public ByteBuffer map(int size) {
		if (size > this.size) {
			resize(size);
		}
		if (mapped) {
			throw new IllegalStateException("Buffer is already mapped");
		}
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		ByteBuffer oldBuffer = glMapBufferRange(GL_ARRAY_BUFFER,
			0,
			size,
			GL_MAP_WRITE_BIT |
				GL_MAP_INVALIDATE_BUFFER_BIT |
				GL_MAP_FLUSH_EXPLICIT_BIT |
				GL_MAP_UNSYNCHRONIZED_BIT |
				0,
			this.oldBuffer);
		mapped = true;
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		Objects.requireNonNull(oldBuffer);
		return this.oldBuffer = oldBuffer;
	}

	public void unmap(int size) {
		if (size > this.size) {
			throw new IllegalArgumentException("Unmapping more bytes than buffer size: " + size + " > " + this.size);
		}
		if (!mapped) {
			throw new IllegalStateException("Buffer is not mapped");
		}
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glFlushMappedBufferRange(GL_ARRAY_BUFFER, 0, size);
		glUnmapBuffer(GL_ARRAY_BUFFER);
		mapped = false;
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
}
