package fun.qu_an.minecraft.asyncparticles.client.particle.buffer;

import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

import java.nio.ByteBuffer;
import java.util.Objects;


public class ParticleStorageBuffer {
	public final int ssbo;
	private ByteBuffer oldBuffer;
	private int size;
	private boolean mapped = false;

	public ParticleStorageBuffer() {
		this.ssbo = GL15C.glGenBuffers();
	}

	public ParticleStorageBuffer(int size) {
		this();
		resize(size);
	}

	/**
	 * 绑定 SSBO 到 GL_SHADER_STORAGE_BUFFER（用于 map/unmap）
	 */
	public void bind() {
		GLCaps.csSupport.glBindShaderStorageBuffer(ssbo);
	}

	/**
	 * 解绑
	 */
	public static void unbind() {
		GLCaps.csSupport.glBindShaderStorageBuffer(0);
	}

	/**
	 * 绑定到指定的 shader binding point（用于 compute 或 fragment shader 访问）
	 */
	public void bindBase(int bindingPoint) {
		GLCaps.csSupport.glBindShaderStorageBufferBase(bindingPoint, ssbo);
	}

	/**
	 * 调整缓冲区大小（仅当新 size 更大时）
	 *
	 * @return 是否实际发生了 resize
	 */
	public boolean resize(int size) {
		if (this.size >= size) {
			return false;
		}
		resize0(size);
		return true;
	}

	/**
	 * 强制调整缓冲区大小（内部使用）
	 */
	public void resize0(int size) {
		bind();
		// 使用 GL_DYNAMIC_COPY 或 GL_DYNAMIC_READ 更合适（GPU 读写，CPU 可能读）
		// 但 map 写入时 GL_DYNAMIC_DRAW 也可接受
		GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, size, GL15C.GL_DYNAMIC_DRAW);
		GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
		this.size = size;
		unbind();
	}

	/**
	 * 映射缓冲区用于写入（高效 CPU → GPU 传输）
	 */
	public ByteBuffer map(int size) {
		if (size > this.size) {
			resize(size);
		}
		if (mapped) {
			throw new IllegalStateException("Buffer is already mapped");
		}
		bind();
		// 注意：SSBO 通常不需要 GL_MAP_READ_BIT，除非你要读回
		ByteBuffer buffer = GL30C.glMapBufferRange(
			GL43C.GL_SHADER_STORAGE_BUFFER,
			0,
			size,
			GL30C.GL_MAP_WRITE_BIT |
				GL30C.GL_MAP_INVALIDATE_BUFFER_BIT |   // 丢弃旧内容，提升性能
				GL30C.GL_MAP_FLUSH_EXPLICIT_BIT |      // 需手动 flush
				GL30C.GL_MAP_UNSYNCHRONIZED_BIT |        // 不等待 GPU，需自行同步
				0,
			oldBuffer
		);
		mapped = true;
		GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
		Objects.requireNonNull(buffer, "Failed to map SSBO");
		unbind();
		return this.oldBuffer = buffer;
	}

	/**
	 * 解映射缓冲区，并显式 flush 写入范围
	 */
	public void unmap(int size) {
		if (size > this.size) {
			throw new IllegalArgumentException("Unmapping more bytes than buffer size: " + size + " > " + this.size);
		}
		if (!mapped) {
			throw new IllegalStateException("Buffer is not mapped");
		}
		bind();
		GL30C.glFlushMappedBufferRange(GL43C.GL_SHADER_STORAGE_BUFFER, 0, size);
		GL15C.glUnmapBuffer(GL43C.GL_SHADER_STORAGE_BUFFER);
		mapped = false;
		GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
		unbind();
	}

	public void delete() {
		GL15C.glDeleteBuffers(ssbo);
	}

	public int getSize() {
		return size;
	}
}
