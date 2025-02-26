package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;

import java.io.IOException;

public class AsyncparticlesClient {
	public static final String MOD_ID = "asyncparticles";

	public static void init() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
