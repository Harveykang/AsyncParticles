package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FakeBufferBuilder extends BufferBuilder {

	public static final FakeBufferBuilder INSTANCE = new FakeBufferBuilder();

	protected FakeBufferBuilder() {
		super(0);
	}

	public void begin(@NotNull VertexFormat.Mode mode, @NotNull VertexFormat format) {
	}

	@Override
	public void setQuadSorting(VertexSorting quadSorting) {
	}

	@Override
	public void restoreSortState(SortState sortState) {
	}

	@Override
	public void putByte(int index, byte byteValue) {
	}

	@Override
	public void putShort(int index, short shortValue) {
	}

	@Override
	public void putFloat(int index, float floatValue) {
	}

	@Override
	public void endVertex() {
	}

	@Override
	public void nextElement() {
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
	}

	public void clear() {
	}

	public void discard() {
	}

	public SortState getSortState() {
		throw new UnsupportedOperationException("Cannot get sort state of a fake buffer builder");
	}

	public boolean isCurrentBatchEmpty() {
		return true;
	}

	@Nullable
	public BufferBuilder.RenderedBuffer endOrDiscardIfEmpty() {
		throw new UnsupportedOperationException("Cannot end or discard a fake buffer builder");
	}

	public RenderedBuffer end() {
		throw new UnsupportedOperationException("Cannot end a fake buffer builder");
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		throw new UnsupportedOperationException("Cannot set color in a fake buffer builder");
	}

	@Override
	public VertexFormatElement currentElement() {
		throw new UnsupportedOperationException("Cannot get the current element in a fake buffer builder");
	}

	public boolean building() {
		return false;
	}
}
