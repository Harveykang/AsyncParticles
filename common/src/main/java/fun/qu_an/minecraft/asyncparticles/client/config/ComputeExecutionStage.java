package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum ComputeExecutionStage implements TranslatableEnum {
//	LEVEL_EXTRACTION(() -> Component.translatable("config.asyncparticles.enum.ComputeExecutionStage.LEVEL_EXTRACTION")),
	LEVEL_RENDERING(() -> Component.translatable("config.asyncparticles.enum.ComputeExecutionStage.LEVEL_RENDERING")),
	PARTICLE_RENDERING(() -> Component.translatable("config.asyncparticles.enum.ComputeExecutionStage.PARTICLE_RENDERING"));
	private final Supplier<Component> componentSupplier;

	ComputeExecutionStage(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
