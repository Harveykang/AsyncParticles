package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

public enum FailBehavior implements TranslatableEnum {
	RAISE_EXCEPTION(Component.translatable("config.asyncparticles.enum.FailBehavior.RAISE_EXCEPTION")),
	MARK_AS_SYNC(Component.translatable("config.asyncparticles.enum.FailBehavior.MARK_AS_SYNC"));
	private final Component translatable;

	FailBehavior(Component translatable) {
		this.translatable = translatable;
	}

	@Override
	public Component getTranslatable() {
		return translatable;
	}
}
