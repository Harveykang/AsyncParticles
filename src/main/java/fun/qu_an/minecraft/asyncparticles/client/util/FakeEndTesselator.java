package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.Tesselator;
import org.lwjgl.system.MemoryUtil;

public class FakeEndTesselator extends Tesselator {
	public static final FakeEndTesselator INSTANCE = new FakeEndTesselator();
	private Runnable endRunnable;

	private FakeEndTesselator() {
		super(0);
		MemoryUtil.memFree(builder.buffer);
		builder.buffer = null;
	}

	@Override
	public void end() {
		endRunnable.run();
		endRunnable = null;
		// do nothing
	}

	public Tesselator onEnd(Runnable runnable) {
		this.endRunnable = runnable;
		return this;
	}
}
