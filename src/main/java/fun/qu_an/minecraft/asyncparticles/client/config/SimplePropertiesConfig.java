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
	public static boolean forceSyncLevelRenderMarkDirty = false;
	public static boolean forceDoneBlockAnimateTick = false;
	public static boolean forceDoneParticleTick = false;
	public static boolean forceDoneTextureTick = false;
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
		forceSyncLevelRenderMarkDirty = getBoolean(properties, "forceSyncLevelRenderMarkDirty", false);
		forceDoneBlockAnimateTick = getBoolean(properties, "forceDoneBlockAnimateTick", false);
		forceDoneParticleTick = getBoolean(properties, "forceDoneParticleTick", false);
		forceDoneTextureTick = getBoolean(properties, "forceDoneTextureTick", false);

		if (shouldSave) {
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
			shouldSave = false;
		}
	}

	private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
		String b = properties.getProperty(key);
		if (b != null) {
			return !Boolean.toString(!defaultValue).equalsIgnoreCase(b);
		} else {
			properties.setProperty(key, String.valueOf(defaultValue));
			shouldSave = true;
			return defaultValue;
		}
	}

	@FunctionalInterface
	private interface BooleanPropertyConsumer<T> {
		void accept(T value, boolean shouldSave);
	}
}
