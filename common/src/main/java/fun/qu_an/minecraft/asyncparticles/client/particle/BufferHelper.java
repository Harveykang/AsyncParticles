package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.platform.MemoryTracker;
import it.unimi.dsi.fastutil.HashCommon;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class BufferHelper {
	private ByteBuffer buffer;
	private boolean building = false;

	public BufferHelper() {
		buffer = MemoryTracker.create(8);
	}

	public void ensureCapacity(int expectedCapacity) {
		int capacity = buffer.capacity();
		if (expectedCapacity > capacity) {
			int newCapacity = HashCommon.nextPowerOfTwo(expectedCapacity);
			int oldPosition = buffer.position();
			ByteBuffer newBuffer = MemoryTracker.resize(buffer, newCapacity);
			newBuffer.position(oldPosition);
			buffer = newBuffer;
		}
	}

	public void ensureWritableBytes(int bytes) {
		ensureCapacity(buffer.position() + bytes);
	}

	public void put(byte b) {
		ensureWritableBytes(1);
		buffer.put(b);
	}

	public void put(short s) {
		ensureWritableBytes(2);
		buffer.putShort(s);
	}

	public void put(int i) {
		ensureWritableBytes(4);
		buffer.putInt(i);
	}

	public void put(long l) {
		ensureWritableBytes(8);
		buffer.putLong(l);
	}

	public void put(float f) {
		ensureWritableBytes(4);
		buffer.putFloat(f);
	}

	public void put(double d) {
		ensureWritableBytes(8);
		buffer.putDouble(d);
	}

	public boolean isBuilding() {
		return building;
	}

	public void begin() {
		if (isBuilding()) {
			throw new IllegalStateException("Already began!");
		}
		building = true;
	}

	public ByteBuffer endAndFlip() {
		if (!isBuilding()) {
			throw new IllegalStateException("Not began!");
		}
		try {
			return buffer.flip();
		} finally {
			building = false;
		}
	}

	public void clear() {
		buffer.clear();
	}

	public void copyFrom(long address, int bytes) {
		int newPosition = buffer.position() + bytes;
		ensureCapacity(newPosition);
		MemoryUtil.memCopy(address, MemoryUtil.memAddress(buffer), bytes);
		buffer.position(newPosition);
	}

	public void copyUnsafeFrom(long address, int bytes) {
		MemoryUtil.memCopy(address, MemoryUtil.memAddress(buffer), bytes);
		buffer.position(buffer.position() + bytes);
	}
}
