package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum ParticleAsyncMode implements TranslatableEnum {
	SEQUENTIAL(() -> Component.translatable("config.asyncparticles.enum.ParticleAsyncMode.SEQUENTIAL")
		.withStyle(ChatFormatting.GREEN)),
	SPLIT(() -> Component.translatable("config.asyncparticles.enum.ParticleAsyncMode.SPLIT")
		.withStyle(ChatFormatting.YELLOW)),
	DISABLE(() -> Component.translatable("config.asyncparticles.enum.ParticleAsyncMode.DISABLE")
		.withStyle(ChatFormatting.RED));
	private final Supplier<Component> componentSupplier;

	ParticleAsyncMode(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
