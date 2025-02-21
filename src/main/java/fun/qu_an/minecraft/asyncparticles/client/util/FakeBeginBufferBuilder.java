package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class FakeBeginBufferBuilder extends BufferBuilder {
	public static final FakeBeginBufferBuilder INSTANCE = new FakeBeginBufferBuilder(0);

	private FakeBeginBufferBuilder(int capacity) {
		super(0);
	}

	@Override
	public void begin(VertexFormat.@NotNull Mode mode, @NotNull VertexFormat format) {
		// do nothing
	}
}
