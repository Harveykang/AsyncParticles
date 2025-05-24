package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

@FunctionalInterface
public interface MinecraftConsumer extends Consumer<Minecraft> {
	@Override
	void accept(Minecraft mc);
}
