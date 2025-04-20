package fun.qu_an.minecraft.asyncparticles.client.util;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class BindingTesselator {
	public static final BindingTesselator EMPTY = new BindingTesselator() {
		@Override
		public @NotNull BufferBuilder begin() {
			return FakeBufferBuilder.INSTANCE;
		}

		@Override
		public BufferBuilder getBuilder() {
			return FakeBufferBuilder.INSTANCE;
		}

		@Override
		public void clear() {
			// do nothing
		}

		@Override
		public void close() {
			// do nothing
		}
	};
	@NotNull
	private final VertexFormat.Mode mode;
	@NotNull
	private final VertexFormat format;
	private BufferBuilder builder;
	@NotNull
	public final ByteBufferBuilder buffer;

	public BindingTesselator(int capacity, @NotNull VertexFormat.Mode mode, @NotNull VertexFormat format) {
		this.buffer = new ByteBufferBuilder(capacity);
		this.mode = mode;
		this.format = format;
	}

	@SuppressWarnings("DataFlowIssue")
	private BindingTesselator() {
		this.buffer = null;
		this.mode = null;
		this.format = null;
	}

	public @NotNull BufferBuilder begin() {
		BufferBuilder builder = this.builder;
		if (builder != null && builder.building) {
			return builder;
		}
		return this.builder = new BufferBuilder(this.buffer, mode, format);
	}

	public BufferBuilder getBuilder() {
		return this.builder;
	}

	public void clear() {
		BufferBuilder builder = this.builder;
		if (builder != null) {
			buffer.discard();
			this.builder = null;
		}
	}

	public void close() {
		buffer.close();
		this.builder = null;
	}
}
