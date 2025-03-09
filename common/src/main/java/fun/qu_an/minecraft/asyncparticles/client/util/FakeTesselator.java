package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import org.jetbrains.annotations.NotNull;

public class FakeTesselator extends Tesselator {
	public static final FakeTesselator INSTANCE = new FakeTesselator();

	protected FakeTesselator() {
		super(0);
	}

	public void end() {
		// Do nothing
	}

	public @NotNull BufferBuilder getBuilder() {
		return FakeBufferBuilder.INSTANCE;
	}
}
