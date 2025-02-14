package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.irisshaders.iris.Iris;
import net.mehvahdjukaar.dummmmmmy.DummmmmmyClient;

import java.io.IOException;

public class AsyncparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncparticles";

	@Override
	public void onInitializeClient() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
