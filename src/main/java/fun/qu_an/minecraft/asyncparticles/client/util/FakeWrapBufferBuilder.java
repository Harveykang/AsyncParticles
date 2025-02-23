package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.Nullable;

public class FakeWrapBufferBuilder extends FakeBufferBuilder {
	private final BufferBuilder wrapped;

	public FakeWrapBufferBuilder(BufferBuilder wrapped) {
		super();
		this.wrapped = wrapped;
	}

	public void setQuadSorting(VertexSorting quadSorting) {
		wrapped.setQuadSorting(quadSorting);
	}

	public SortState getSortState() {
		return wrapped.getSortState();
	}

	public void restoreSortState(SortState sortState) {
		wrapped.restoreSortState(sortState);
	}

	public void begin(VertexFormat.Mode mode, VertexFormat format) {
		if (wrapped.mode != mode || wrapped.format != format) {
			throw new UnsupportedOperationException("Cannot change mode or format of a fake buffer builder");
		}
	}

	public boolean isCurrentBatchEmpty() {
		return wrapped.isCurrentBatchEmpty();
	}

	@Nullable
	public BufferBuilder.RenderedBuffer endOrDiscardIfEmpty() {
		return wrapped.endOrDiscardIfEmpty();
	}

	public RenderedBuffer end() {
		throw new UnsupportedOperationException("Cannot end a fake buffer builder");
	}

	@Override
	public void putByte(int index, byte byteValue) {
		wrapped.putByte(index, byteValue);
	}

	@Override
	public void putShort(int index, short shortValue) {
		wrapped.putShort(index, shortValue);
	}

	@Override
	public void putFloat(int index, float floatValue) {
		wrapped.putFloat(index, floatValue);
	}

	@Override
	public void endVertex() {
		wrapped.endVertex();
	}

	@Override
	public void nextElement() {
		wrapped.nextElement();
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		return wrapped.color(red, green, blue, alpha);
	}

	@Override
	public void vertex(
		float x,
		float y,
		float z,
		float red,
		float green,
		float blue,
		float alpha,
		float texU,
		float texV,
		int overlayUV,
		int lightmapUV,
		float normalX,
		float normalY,
		float normalZ
	) {
		wrapped.vertex(x, y, z, red, green, blue, alpha, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ);
	}

	public void clear() {
		throw new UnsupportedOperationException("Cannot clear a fake buffer builder");
	}

	public void discard() {
		throw new UnsupportedOperationException("Cannot discard a fake buffer builder");
	}

	@Override
	public VertexFormatElement currentElement() {
		return wrapped.currentElement();
	}

	public boolean building() {
		return wrapped.building();
	}
}
