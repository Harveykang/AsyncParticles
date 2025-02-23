package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class FakeTesselator extends Tesselator {
	public static final FakeTesselator INSTANCE = new FakeTesselator();

	protected FakeTesselator() {
		super(0);
	}

	public void end() {
		// Do nothing
	}

	public @NotNull BufferBuilder getBuilder() {
		throw new UnsupportedOperationException("FakeTesselator does not support getBuilder()");
	}
}
