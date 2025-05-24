package fun.qu_an.minecraft.asyncparticles.client.api;

import net.minecraft.client.multiplayer.ClientLevel;

import java.util.function.Consumer;

@FunctionalInterface
public interface ClientLevelConsumer extends Consumer<ClientLevel> {
	@Override
	void accept(ClientLevel level);
}
