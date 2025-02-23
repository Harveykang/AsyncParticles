package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.Nullable;

public class FakeBufferBuilder extends BufferBuilder {
	public FakeBufferBuilder() {
		super(0);
	}

	public void setQuadSorting(VertexSorting quadSorting) {
		throw new UnsupportedOperationException("Cannot set sort state of a fake buffer builder");
	}

	public BufferBuilder.SortState getSortState() {
		throw new UnsupportedOperationException("Cannot get sort state of a fake buffer builder");
	}

	public void restoreSortState(BufferBuilder.SortState sortState) {
		throw new UnsupportedOperationException("Cannot restore sort state of a fake buffer builder");
	}

	@SuppressWarnings("DataFlowIssue")
	public void begin(@Nullable VertexFormat.Mode mode, @Nullable VertexFormat format) {
		this.mode = mode;
		this.format = format;
	}

	public VertexFormat	getFormat() {
		return format;
	}

	public VertexFormat.Mode getVertexFormatMode() {
		return mode;
	}

	public boolean isCurrentBatchEmpty() {
		throw new UnsupportedOperationException("Cannot check if a fake buffer builder is empty");
	}

	@Nullable
	public BufferBuilder.RenderedBuffer endOrDiscardIfEmpty() {
		throw new UnsupportedOperationException("Cannot end or discard a fake buffer builder");
	}

	public BufferBuilder.RenderedBuffer end() {
		throw new UnsupportedOperationException("Cannot end a fake buffer builder");
	}

	@Override
	public void putByte(int index, byte byteValue) {
		throw new UnsupportedOperationException("Cannot put a byte to a fake buffer builder");
	}

	@Override
	public void putShort(int index, short shortValue) {
		throw new UnsupportedOperationException("Cannot put a short to a fake buffer builder");
	}

	@Override
	public void putFloat(int index, float floatValue) {
		throw new UnsupportedOperationException("Cannot put a float to a fake buffer builder");
	}

	@Override
	public void endVertex() {
		throw new UnsupportedOperationException("Cannot end a vertex in a fake buffer builder");
	}

	@Override
	public void nextElement() {
		throw new UnsupportedOperationException("Cannot advance to the next element in a fake buffer builder");
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		throw new UnsupportedOperationException("Cannot set color in a fake buffer builder");
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
		throw new UnsupportedOperationException("Cannot add a vertex to a fake buffer builder");
	}

	public void clear() {
		throw new UnsupportedOperationException("Cannot clear a fake buffer builder");
	}

	public void discard() {
		throw new UnsupportedOperationException("Cannot discard a fake buffer builder");
	}

	@Override
	public VertexFormatElement currentElement() {
		throw new UnsupportedOperationException("Cannot get the current element in a fake buffer builder");
	}

	public boolean building() {
		return false;
	}
}
