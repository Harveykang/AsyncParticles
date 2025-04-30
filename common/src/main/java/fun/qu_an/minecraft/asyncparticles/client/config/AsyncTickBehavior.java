package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

public enum AsyncTickBehavior implements TranslatableEnum {
	CANCELLABLE(Component.translatable("config.asyncparticles.enum.AsyncTickBehavior.CANCELLABLE")),
	FORCE_COMPLETE(Component.translatable("config.asyncparticles.enum.AsyncTickBehavior.FORCE_COMPLETE")),
	DISABLED(Component.translatable("config.asyncparticles.enum.AsyncTickBehavior.DISABLED"));
	private final Component translatable;

	AsyncTickBehavior(Component translatable) {
		this.translatable = translatable;
	}

	@Override
	public Component getTranslatable() {
		return translatable;
	}
}
