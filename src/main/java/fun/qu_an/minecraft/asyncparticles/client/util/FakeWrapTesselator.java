package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class FakeWrapTesselator extends FakeTesselator {
	private final @NotNull BufferBuilder wrapped;

	public FakeWrapTesselator(@NotNull BufferBuilder wrapped) {
		super();
		this.wrapped = wrapped;
	}

	public void end() {
		// Do nothing
	}

	public @NotNull BufferBuilder getBuilder() {
		return wrapped;
	}
}
