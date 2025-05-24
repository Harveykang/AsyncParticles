package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MinecraftConsumer {
	void accept(Minecraft mc);
}
