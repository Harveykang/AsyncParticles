package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FakeBufferBuilder extends BufferBuilder {
	public static final FakeBufferBuilder INSTANCE = new FakeBufferBuilder();
	private FakeBufferBuilder() {
		super(null, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
	}

	@Override
	@Nullable
	public MeshData build() {
		return null;
	}

	@Override
	public @NotNull MeshData buildOrThrow() {
		throw new UnsupportedOperationException("Cannot build mesh or throw in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer addVertex(float x, float y, float z) {
		throw new UnsupportedOperationException("Cannot add vertex in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
		throw new UnsupportedOperationException("Cannot set color in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setColor(int color) {
		throw new UnsupportedOperationException("Cannot set color in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setUv(float u, float v) {
		throw new UnsupportedOperationException("Cannot set uv in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setUv1(int u, int v) {
		throw new UnsupportedOperationException("Cannot set uv1 in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setOverlay(int packedOverlay) {
		throw new UnsupportedOperationException("Cannot set overlay in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setUv2(int u, int v) {
		throw new UnsupportedOperationException("Cannot set uv2 in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setLight(int packedLight) {
		throw new UnsupportedOperationException("Cannot set light in FakeBufferBuilder");
	}

	@Override
	public @NotNull VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
		throw new UnsupportedOperationException("Cannot set normal in FakeBufferBuilder");
	}

	@Override
	public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ) {
		throw new UnsupportedOperationException("Cannot add vertex in FakeBufferBuilder");
	}
}
