package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.client.multiplayer.ClientLevel;

@FunctionalInterface
public interface ClientLevelConsumer {
	void accept(ClientLevel level);
}
