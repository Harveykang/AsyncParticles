package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class TryAndStoreFakeBufferBuilder extends FakeBufferBuilder {
	public TryAndStoreFakeBufferBuilder() {
		super();
	}

	public void begin(@NotNull VertexFormat.Mode mode, @NotNull VertexFormat format) {
		this.mode = mode;
		this.format = format;
	}

	public VertexFormat	getFormat() {
		return format;
	}

	public VertexFormat.Mode getVertexFormatMode() {
		return mode;
	}
}
