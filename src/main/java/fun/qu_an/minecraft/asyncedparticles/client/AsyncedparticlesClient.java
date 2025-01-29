package fun.qu_an.minecraft.asyncedparticles.client;

import fun.qu_an.minecraft.asyncedparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.ErrorManager;

public class AsyncedparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncedparticles";
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeClient() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
