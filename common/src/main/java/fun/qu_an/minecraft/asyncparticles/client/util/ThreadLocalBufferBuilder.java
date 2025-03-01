package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ThreadLocalBufferBuilder implements BufferVertexConsumer {
	private volatile ThreadLocal<BufferBuilder> buffer;
	private final Set<BufferBuilder> bufferSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final int size;

	public ThreadLocalBufferBuilder(int size, int threads) {
		this.size = (size / threads) * 2;
	}

	@Override
	public @NotNull VertexFormatElement currentElement() {
		return buffer.get().currentElement();
	}

	@Override
	public void nextElement() {
		buffer.get().nextElement();
	}

	@Override
	public void putByte(int i, byte b) {
		buffer.get().putByte(i, b);
	}

	@Override
	public void putShort(int i, short s) {
		buffer.get().putShort(i, s);
	}

	@Override
	public void putFloat(int i, float f) {
		buffer.get().putFloat(i, f);
	}

	@Override
	public @NotNull VertexConsumer vertex(double d, double e, double f) {
		return buffer.get().vertex(d, e, f);
	}

	@Override
	public @NotNull VertexConsumer color(int i, int j, int k, int l) {
		return buffer.get().color(i, j, k, l);
	}

	@Override
	public @NotNull VertexConsumer uv(float f, float g) {
		return buffer.get().uv(f, g);
	}

	@Override
	public @NotNull VertexConsumer overlayCoords(int i, int j) {
		return buffer.get().overlayCoords(i, j);
	}

	@Override
	public @NotNull VertexConsumer uv2(int i, int j) {
		return buffer.get().uv2(i, j);
	}

	@Override
	public @NotNull VertexConsumer normal(float f, float g, float h) {
		return buffer.get().normal(f, g, h);
	}

	@Override
	public void endVertex() {
		buffer.get().endVertex();
	}

	@Override
	public void defaultColor(int i, int j, int k, int l) {
		buffer.get().defaultColor(i, j, k, l);
	}

	@Override
	public void unsetDefaultColor() {
		buffer.get().unsetDefaultColor();
	}

	public @Nullable BufferBuilder.RenderedBuffer end() {
		BufferBuilder builder = buffer.get();
		if (builder == null) {
			return null;
		}
		try {
			return builder.end();
		} finally {
			bufferSet.remove(builder);
			buffer.remove();
		}
	}

	public void endAll(Consumer<BufferBuilder.RenderedBuffer> consumer) {
		if (!RenderSystem.isOnRenderThread()) {
			throw new IllegalStateException("Cannot call end() outside of render thread");
		}
		bufferSet.forEach(bufferBuilder -> consumer.accept(bufferBuilder.end()));
		bufferSet.clear();
		this.buffer = null;
	}

	public void begin(VertexFormat.Mode mode, VertexFormat format) {
		this.buffer = ThreadLocal.withInitial(() -> {
			BufferBuilder builder = new BufferBuilder(size);
			builder.begin(mode, format);
			bufferSet.add(builder);
			return builder;
		});
	}

//	public void setQuadSorting(VertexSorting vertexSorting) {
//		bufferSet.forEach(bufferBuilder -> bufferBuilder.setQuadSorting(vertexSorting));
//	}

	public @Nullable BufferBuilder pollBuffer() {
		try {
			return buffer.get();
		} finally {
			bufferSet.remove(buffer.get());
			buffer.remove();
		}
	}
}
