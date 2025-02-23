package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.Tesselator;

public class CustomableEndTesselator extends FakeTesselator {
	public static final CustomableEndTesselator INSTANCE = new CustomableEndTesselator();
	private Runnable endRunnable;

	public CustomableEndTesselator() {
		super();
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
