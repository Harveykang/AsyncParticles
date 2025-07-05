package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.util.TranslatableEnum;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public enum ParticleCullingMode implements TranslatableEnum {
	DISABLED(() -> Component.translatable("config.asyncparticles.enum.ParticleCullingShape.DISABLED")
		.withStyle(ChatFormatting.RED)),
	SPHERE(() -> Component.translatable("config.asyncparticles.enum.ParticleCullingShape.SPHERE")
		.withStyle(ChatFormatting.GREEN)),
	AABB(() -> Component.translatable("config.asyncparticles.enum.ParticleCullingShape.AABB")
		.withStyle(ChatFormatting.YELLOW)),
	ASYNC_SPHERE(() -> Component.translatable("config.asyncparticles.enum.ParticleCullingShape.ASYNC_SPHERE")
		.withStyle(ChatFormatting.GREEN)),
	ASYNC_AABB(() -> Component.translatable("config.asyncparticles.enum.ParticleCullingShape.ASYNC_AABB")
		.withStyle(ChatFormatting.YELLOW));
	private final Supplier<Component> componentSupplier;

	ParticleCullingMode(Supplier<Component> componentSupplier) {
		this.componentSupplier = componentSupplier;
	}

	@Override
	public Component getComponent() {
		return componentSupplier.get();
	}
}
