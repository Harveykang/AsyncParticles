package fun.qu_an.minecraft.asyncparticles.client.compat.cooparticlesapi;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum CooTickMode implements TranslatableEnum {
	SYNCHRONOUSLY(() -> Component.translatable("config.asyncparticles.enum.cooparticles.CooTickMode.SYNCHRONOUSLY")),
	ASYNC_IN_PARALLEL(() -> Component.translatable("config.asyncparticles.enum.cooparticles.CooTickMode.ASYNC_IN_PARALLEL")),
	ASYNC_IN_SEQUENCED(() -> Component.translatable("config.asyncparticles.enum.cooparticles.CooTickMode.ASYNC_IN_SEQUENCED"));

	private final Supplier<Component> component;

	CooTickMode(Supplier<Component> component) {
		this.component = component;
	}

	@Override
	public Component getComponent() {
		return component.get();
	}
}
