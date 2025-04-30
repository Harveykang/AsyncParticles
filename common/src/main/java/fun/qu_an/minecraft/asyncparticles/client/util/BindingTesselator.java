package fun.qu_an.minecraft.asyncparticles.client.util;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class BindingTesselator extends Tesselator {
	public static final BindingTesselator EMPTY = new BindingTesselator() {
		@Override
		public @NotNull BufferBuilder begin() {
			return FakeBufferBuilder.INSTANCE;
		}

		@Override
		public @NotNull BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
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
	public final VertexFormat.Mode mode;
	@NotNull
	public final VertexFormat format;
	public final boolean shouldSync;
	private BufferBuilder builder;

	public BindingTesselator(int capacity, @NotNull VertexFormat.Mode mode, @NotNull VertexFormat format, boolean shouldSync) {
		super(capacity);
		this.mode = mode;
		this.format = format;
		this.shouldSync = shouldSync;
	}

	@SuppressWarnings("DataFlowIssue")
	private BindingTesselator() {
		super(0);
		this.mode = null;
		this.format = null;
		this.shouldSync = true;
	}

	public @NotNull BufferBuilder begin() {
		BufferBuilder builder = this.builder;
		if (builder != null && builder.building) {
			return builder;
		}
		return this.builder = new BufferBuilder(this.buffer, mode, format);
	}

	@Override
	public @NotNull BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
		BufferBuilder builder = this.builder;
		if (builder != null && builder.building) {
			return builder;
		}
		if (mode != this.mode || format != this.format) {
			throw new IllegalStateException("Cannot begin with different format or mode");
		}
		return this.builder = new BufferBuilder(this.buffer, mode, format);
	}

	public BufferBuilder getBuilder() {
		return builder;
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
