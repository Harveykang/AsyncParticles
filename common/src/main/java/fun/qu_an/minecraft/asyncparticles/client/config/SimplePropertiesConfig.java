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
	public static boolean asyncClientBlockEntityTick = true;
	public static boolean greedyAsyncClientBlockEntityTick = false;
	public static boolean asyncClientBlockEntityAnimate = true;
	public static boolean forceSyncLevelRenderMarkDirty = false;
	public static boolean forceDoneBlockAnimateTick = false;
	public static boolean forceDoneParticleTick = false;
	public static boolean forceDoneTextureTick = false;
	public static boolean markSyncIfTickFailed = false;
	public static boolean ignoreParticleTickExceptions = false;
	private static boolean shouldSave;

	public static void load() throws IOException {
		Properties properties = new Properties();
		if (!Files.exists(CONFIG_FILE)) {
			Files.createDirectories(CONFIG_FILE.getParent());
			Files.createFile(CONFIG_FILE);
		} else {
			properties.load(Files.newInputStream(CONFIG_FILE));
		}

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

		asyncClientBlockEntityTick = getBoolean(properties, "asyncClientBlockEntityTick", true);
		greedyAsyncClientBlockEntityTick = getBoolean(properties, "greedyAsyncClientBlockEntityTick", false);
		asyncClientBlockEntityAnimate = getBoolean(properties, "asyncClientBlockEntityAnimate", true);
		forceSyncLevelRenderMarkDirty = getBoolean(properties, "forceSyncLevelRenderMarkDirty", false);
		forceDoneBlockAnimateTick = getBoolean(properties, "forceDoneBlockAnimateTick", false);
		forceDoneParticleTick = getBoolean(properties, "forceDoneParticleTick", false);
		forceDoneTextureTick = getBoolean(properties, "forceDoneTextureTick", false);
		markSyncIfTickFailed = getBoolean(properties, "markSyncIfTickFailed", false);
		ignoreParticleTickExceptions = getBoolean(properties, "ignoreParticleTickExceptions", false);

		if (shouldSave) {
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
			shouldSave = false;
		}
	}

	private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
		String b = properties.getProperty(key);
		if (b != null) {
			// !Boolean.toString(!defaultValue).equalsIgnoreCase(b) ? defaultValue : !defaultValue;
			return Boolean.toString(!defaultValue).equalsIgnoreCase(b) != defaultValue;
		} else {
			properties.setProperty(key, String.valueOf(defaultValue));
			shouldSave = true;
			return defaultValue;
		}
	}
}
