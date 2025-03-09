package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CustomTesselator extends FakeTesselator {
	private final Consumer<BufferBuilder> endConsumer;
	private final BufferBuilder bufferBuilder;

	private CustomTesselator(BufferBuilder bufferBuilder, Consumer<BufferBuilder> consumer) {
		super();
		this.bufferBuilder = bufferBuilder;
		this.endConsumer = consumer;
	}

	public static CustomTesselator of(BufferBuilder bufferBuilder, Consumer<BufferBuilder> consumer) {
		return new CustomTesselator(bufferBuilder, consumer);
	}

	@Override
	public void end() {
		endConsumer.accept(bufferBuilder);
		// do nothing
	}

	@Override
	public @NotNull BufferBuilder getBuilder() {
		return bufferBuilder;
	}
}
