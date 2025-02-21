package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.Tesselator;

public class FakeEndTesselator extends Tesselator {
	public static final FakeEndTesselator INSTANCE = new FakeEndTesselator();
	private FakeEndTesselator() {
		super(0);
	}

	@Override
	public void end() {
		// do nothing
	}
}
