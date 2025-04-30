package fun.qu_an.minecraft.asyncparticles.client.util;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class TryAndStoreFakeBufferBuilder extends FakeTesselator {
	private VertexFormat.Mode mode;
	private VertexFormat format;

	public TryAndStoreFakeBufferBuilder() {
	}

	@Override
	public @NotNull BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
		this.mode = mode;
		this.format = format;
		return FakeBufferBuilder.INSTANCE;
	}

	public VertexFormat.Mode getMode() {
		return mode;
	}

	public VertexFormat getFormat() {
		return format;
	}
}
