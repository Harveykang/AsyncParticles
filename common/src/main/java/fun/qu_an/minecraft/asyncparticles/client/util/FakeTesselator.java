package fun.qu_an.minecraft.asyncparticles.client.util;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class FakeTesselator extends Tesselator {
	private static final FakeTesselator INSTANCE = new FakeTesselator();

	public static FakeTesselator getFakeInstance() {
		return INSTANCE;
	}

	protected FakeTesselator() {
		super(0);
	}

	@Override
	public void clear() {
		// Do nothing
	}

	@Override
	public @NotNull BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
		return FakeBufferBuilder.INSTANCE;
	}
}
