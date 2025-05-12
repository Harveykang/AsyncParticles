package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum TickMode implements TranslatableEnum {
	SYNCHRONOUSLY(() -> Component.translatable("config.asyncparticles.enum.TickMode.SYNCHRONOUSLY")
		.withStyle(ChatFormatting.RED)),
	INTERRUPTIBLE(() -> Component.translatable("config.asyncparticles.enum.TickMode.INTERRUPTIBLE")
		.withStyle(ChatFormatting.GREEN)),
	FORCE_COMPLETE(() -> Component.translatable("config.asyncparticles.enum.TickMode.FORCE_COMPLETE")
		.withStyle(ChatFormatting.YELLOW));
	private final Supplier<Component> componentSupplier;

	TickMode(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
