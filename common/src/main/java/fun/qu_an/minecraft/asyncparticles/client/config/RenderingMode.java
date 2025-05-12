package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum RenderingMode implements TranslatableEnum {
	SYNCHRONOUSLY(() -> Component.translatable("config.asyncparticles.enum.RenderingMode.SYNCHRONOUSLY")
		.withStyle(ChatFormatting.RED)),
	DELAYED(() -> Component.translatable("config.asyncparticles.enum.RenderingMode.DELAYED")
		.withStyle(ChatFormatting.GREEN)),
	COMPATIBILITY(() -> Component.translatable("config.asyncparticles.enum.RenderingMode.COMPATIBILITY")
		.withStyle(ChatFormatting.YELLOW));
	private final Supplier<Component> componentSupplier;

	RenderingMode(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
