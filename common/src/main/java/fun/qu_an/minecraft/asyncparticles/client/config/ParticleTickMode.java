package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum ParticleTickMode implements TranslatableEnum {
	SEQUENTIAL(() -> Component.translatable("config.asyncparticles.enum.ParticleTickMode.SEQUENTIAL")
		.withStyle(ChatFormatting.GREEN)),
	SPLIT(() -> Component.translatable("config.asyncparticles.enum.ParticleTickMode.SPLIT")
		.withStyle(ChatFormatting.YELLOW)),
	DISABLE(() -> Component.translatable("config.asyncparticles.enum.ParticleTickMode.DISABLE")
		.withStyle(ChatFormatting.RED));
	private final Supplier<Component> componentSupplier;

	ParticleTickMode(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
