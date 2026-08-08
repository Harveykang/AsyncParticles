package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum ParticleCleanupStrategy implements TranslatableEnum {
	PARALLEL_WITH_TICK(() -> Component.translatable("config.asyncparticles.enum.ParticleCleanupStrategy.PARALLEL_WITH_TICK")),
	BLOCK_MAIN_THREAD(() -> Component.translatable("config.asyncparticles.enum.ParticleCleanupStrategy.BLOCK_MAIN_THREAD")),
	MAIN_THREAD(() -> Component.translatable("config.asyncparticles.enum.ParticleCleanupStrategy.MAIN_THREAD"));

	private final Supplier<Component> componentSupplier;

	ParticleCleanupStrategy(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
