package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;
import java.util.Objects;

// TODO: DSA
public class ParticleVertexBuffer {
	public final int vao;
	public final int vbo;
	private ByteBuffer oldBuffer;
	private int size;
	private boolean mapped = false;

	public ParticleVertexBuffer() {
		this.vao = GL30C.glGenVertexArrays();
		this.vbo = GL15C.glGenBuffers();
	}

	public ParticleVertexBuffer(int vao, int vbo) {
		this.vao = vao;
		this.vbo = vbo;
	}

	public static void unbind() {
		GL30C.glBindVertexArray(0);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void bind() {
		GL30C.glBindVertexArray(vao);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
	}

	public boolean resize(int size) {
		if (this.size >= size) {
			return false;
		}
		resize0(size);
		return true;
	}

	public void resize0(int size) {
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, size, GL15C.GL_DYNAMIC_DRAW);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		this.size = size;
	}

	public ByteBuffer map(int size) {
		if (size > this.size) {
			resize(size);
		}
		if (mapped) {
			throw new IllegalStateException("Buffer is already mapped");
		}
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		ByteBuffer oldBuffer = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER,
			0,
			size,
			GL30C.GL_MAP_WRITE_BIT |
				GL30C.GL_MAP_INVALIDATE_BUFFER_BIT |
				GL30C.GL_MAP_FLUSH_EXPLICIT_BIT |
				GL30C.GL_MAP_UNSYNCHRONIZED_BIT |
				0,
			this.oldBuffer);
		mapped = true;
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
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
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, 0, size);
		GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
		mapped = false;
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void delete() {
		GL15C.glDeleteBuffers(vbo);
		GL30C.glDeleteVertexArrays(vao);
	}

	public int getSize() {
		return size;
	}
}
