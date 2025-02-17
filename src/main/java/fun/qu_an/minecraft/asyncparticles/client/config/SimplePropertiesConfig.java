package fun.qu_an.minecraft.asyncparticles.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SimplePropertiesConfig {
	public static final Path CONFIG_FILE = Paths.get("config", "asyncparticles.properties");
	public static final int DEFAULT_LIMIT = 32768;
	public static int limit = DEFAULT_LIMIT;
	public static boolean DEFAULT_ASYNC_BLOCK_ENTITY_TICK = true;
	public static boolean asyncClientBlockEntityTick = true;
	public static boolean asyncParticleCleanup = true;
	public static boolean smoothParticleMotion = true;

	public static void load() throws IOException {
		Properties properties = new Properties();
		if (!Files.exists(CONFIG_FILE)) {
			Files.createDirectories(CONFIG_FILE.getParent());
			Files.createFile(CONFIG_FILE);
			properties.setProperty("limit", String.valueOf(DEFAULT_LIMIT));
			properties.setProperty("asyncClientBlockEntityTick", String.valueOf(DEFAULT_ASYNC_BLOCK_ENTITY_TICK));
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
		} else {
			properties.load(Files.newInputStream(CONFIG_FILE));
		}
		boolean shouldSave = false;
		String limitStr = properties.getProperty("limit");
		int limit;
		try {
			limit = Integer.parseInt(limitStr);
		} catch (NumberFormatException e) {
			properties.setProperty("limit", String.valueOf(DEFAULT_LIMIT));
			shouldSave = true;
			limit = DEFAULT_LIMIT;
		}
		SimplePropertiesConfig.limit = limit;
		
		String asyncClientBlockEntityTickStr = properties.getProperty("asyncClientBlockEntityTick");
		boolean asyncClientBlockEntityTick;
		if (asyncClientBlockEntityTickStr != null) {
			asyncClientBlockEntityTick = Boolean.parseBoolean(asyncClientBlockEntityTickStr);
		} else {
			properties.setProperty("asyncClientBlockEntityTick", String.valueOf(DEFAULT_ASYNC_BLOCK_ENTITY_TICK));
			shouldSave = true;
			asyncClientBlockEntityTick = DEFAULT_ASYNC_BLOCK_ENTITY_TICK;
		}
		SimplePropertiesConfig.asyncClientBlockEntityTick = asyncClientBlockEntityTick;
		
		if (shouldSave) {
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
		}
	}

}
