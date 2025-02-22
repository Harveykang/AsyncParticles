package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

public class FakeBeginBufferBuilder extends BufferBuilder {
	public static final FakeBeginBufferBuilder INSTANCE = new FakeBeginBufferBuilder();

	private FakeBeginBufferBuilder() {
		super(0);
		MemoryUtil.memFree(buffer);
		buffer = null;
	}

	@Override
	public void begin(VertexFormat.@NotNull Mode mode, @NotNull VertexFormat format) {
		// do nothing
	}
}
