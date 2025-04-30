package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum RainEffect implements TranslatableEnum {
	NONE(() -> Component.translatable("config.asyncparticles.enum.RainEffect.NONE")),
	STATIONARY(() -> Component.translatable("config.asyncparticles.enum.RainEffect.STATIONARY")),
	ALWAYS(() -> Component.translatable("config.asyncparticles.enum.RainEffect.ALWAYS"));
	private final Supplier<Component> componentSupplier;

	RainEffect(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
