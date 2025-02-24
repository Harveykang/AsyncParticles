package fun.qu_an.minecraft.asyncparticles.client.fabric;

import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import net.fabricmc.api.ClientModInitializer;

public final class AsyncparticlesClientFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AsyncparticlesClient.init();
	}
}
