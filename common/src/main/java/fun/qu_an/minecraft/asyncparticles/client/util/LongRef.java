package fun.qu_an.minecraft.asyncparticles.client.util;

public class LongRef {
	private long value;

	public LongRef(long value) {
		this.value = value;
	}

	public long get() {
		return value;
	}

	public void set(long value) {
		this.value = value;
	}
}
