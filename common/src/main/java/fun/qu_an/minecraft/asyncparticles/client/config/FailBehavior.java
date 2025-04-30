package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum FailBehavior implements TranslatableEnum {
	RAISE_CRASH(() -> Component.translatable("config.asyncparticles.enum.FailBehavior.RAISE_CRASH")),
	MARK_AS_SYNC(()-> Component.translatable("config.asyncparticles.enum.FailBehavior.MARK_AS_SYNC"));
	private final Supplier<Component> componentSupplier;

	FailBehavior(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
