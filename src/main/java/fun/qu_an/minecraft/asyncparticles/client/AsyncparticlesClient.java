package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.fabricmc.api.ClientModInitializer;

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
