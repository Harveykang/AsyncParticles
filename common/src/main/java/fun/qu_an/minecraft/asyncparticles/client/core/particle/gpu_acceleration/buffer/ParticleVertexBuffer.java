package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer;

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
	private final boolean streamDraw;
	private int mapOffset;

	public ParticleVertexBuffer(boolean streamDraw) {
		this(GL30C.glGenVertexArrays(), GL15C.glGenBuffers(), streamDraw);
	}

	public ParticleVertexBuffer(int vao, boolean streamDraw) {
		this(vao, GL15C.glGenBuffers(), streamDraw);
	}

	public ParticleVertexBuffer(int vao, int vbo, boolean streamDraw) {
		this.vao = vao;
		this.vbo = vbo;
		this.streamDraw = streamDraw;
	}

	public static void unbind() {
		GL30C.glBindVertexArray(0);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void bind() {
		if (vao > 0) {
			GL30C.glBindVertexArray(vao);
		}
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
		GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, size, streamDraw ? GL15C.GL_STREAM_DRAW : GL15C.GL_DYNAMIC_DRAW);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		this.size = size;
	}

	public ByteBuffer map(int size) {
		return map(size, true);
	}

	public ByteBuffer map(int size, boolean invalidateBufferBit) {
		return mapRange(0, size, invalidateBufferBit);
	}

    public ByteBuffer mapRange(int offset, int size, boolean invalidateBufferBit) {
        if (offset + size > this.size) {
			throw new IllegalArgumentException("Range exceeds buffer size: " + (offset + size) + " > " + this.size);
        }
        if (mapped) {
			throw new IllegalStateException("Buffer is already mapped");
        }
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        ByteBuffer buf = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER,
            offset, size,
            GL30C.GL_MAP_WRITE_BIT |
				(invalidateBufferBit ? GL30C.GL_MAP_INVALIDATE_BUFFER_BIT : 0) |
                GL30C.GL_MAP_FLUSH_EXPLICIT_BIT |
                GL30C.GL_MAP_UNSYNCHRONIZED_BIT,
            this.oldBuffer);
        mapped = true;
		mapOffset = offset;
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        Objects.requireNonNull(buf);
        return this.oldBuffer = buf;
    }

	public void flush(int size) {
		if (mapOffset + size > this.size) {
			throw new IllegalArgumentException("Flushing more bytes than buffer size: " + (mapOffset + size) + " > " + this.size);
		}
		if (!mapped) {
			throw new IllegalStateException("Buffer is not mapped");
		}
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, 0, size);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

//	public void flush(int offsetRelatedToMapOffset, int size) {
//		if (mapOffset + size > this.size) {
//			throw new IllegalArgumentException("Flushing more bytes than buffer size: " + (mapOffset + size) + " > " + this.size);
//		}
//		if (!mapped) {
//			throw new IllegalStateException("Buffer is not mapped");
//		}
//		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
//		GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, 0, size);
//		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
//	}

	public void unmap() {
		if (!mapped) {
			throw new IllegalStateException("Buffer is not mapped");
		}
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
		mapped = false;
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void unmap(int offset, int size) {
		if (offset + size > this.size) {
			throw new IllegalArgumentException("Unmapping more bytes than buffer size: " + (offset + size) + " > " + this.size);
		}
		if (!mapped) {
			throw new IllegalStateException("Buffer is not mapped");
		}
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
		GL30C.glFlushMappedBufferRange(GL15C.GL_ARRAY_BUFFER, offset, size);
		GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
		mapped = false;
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void delete() {
		delete(true, true);
	}

	public void delete(boolean deleteVao, boolean deleteVbo) {
		if (mapped) {
			unmap();
		}
		if (deleteVao && vao > 0) {
			GL30C.glDeleteVertexArrays(vao);
		}
		if (deleteVbo && vbo > 0) {
			GL15C.glDeleteBuffers(vbo);
		}
	}

	public int getSize() {
		return size;
	}
}
