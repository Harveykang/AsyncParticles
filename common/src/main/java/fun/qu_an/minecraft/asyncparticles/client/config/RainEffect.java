package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

public enum RainEffect implements TranslatableEnum {
	NONE(Component.translatable("config.asyncparticles.enum.RainEffect.NONE")),
	STATIONARY(Component.translatable("config.asyncparticles.enum.RainEffect.STATIONARY")),
	ALWAYS(Component.translatable("config.asyncparticles.enum.RainEffect.ALWAYS"));

	private final Component translatable;

	RainEffect(Component translatable) {
		this.translatable = translatable;
	}

	@Override
	public Component getTranslatable() {
		return translatable;
	}
}
