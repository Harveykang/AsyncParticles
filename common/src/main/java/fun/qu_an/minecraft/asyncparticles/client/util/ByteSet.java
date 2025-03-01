package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.Arrays;

public class ByteSet {
	protected long[] bytes;
	protected long[] usedFlags;
	protected int size;

	public ByteSet(int size) {
		this.size = size;
		this.bytes = new long[(size + 7) >> 3];
		this.usedFlags = new long[(size + 63) >> 6];
	}

	public void set(int index, byte value) {
		int wordIndex = index >> 3;
		int byteIndex = index & 0xff;
		int wordInUse = index / 64;
		int byteInUse = index % 64;
		if ((usedFlags[wordInUse] & byteInUse) != 0) {
			bytes[wordIndex] &= ~(0xffL << byteIndex);
		}
		bytes[wordIndex] |= (long) value << byteIndex;
		usedFlags[wordInUse] |= 1L << byteInUse;
	}

	public byte get(int index) {
		int wordIndex = index >> 3;
		int bitIndex = index & 0xff;
		return (byte) ((bytes[wordIndex] >> bitIndex) & 0xff);
	}

	public boolean contains(int index) {
		int wordInUse = index >> 6;
		int byteInUse = index & 0x3f;
		return (usedFlags[wordInUse] & byteInUse) != 0;
	}

	public void clear() {
		Arrays.fill(bytes, 0);
		Arrays.fill(usedFlags, 0);
	}
}
