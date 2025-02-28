package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class FakeBeginBufferBuilder extends FakeBufferBuilder {
	public static final FakeBeginBufferBuilder INSTANCE = new FakeBeginBufferBuilder();

	private FakeBeginBufferBuilder() {
		super();
	}

	@Override
	public void begin(VertexFormat.@NotNull Mode mode, @NotNull VertexFormat format) {
		// do nothing
	}
}
