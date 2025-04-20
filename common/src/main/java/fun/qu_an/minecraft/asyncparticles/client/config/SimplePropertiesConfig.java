package fun.qu_an.minecraft.asyncparticles.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SimplePropertiesConfig {
	public static final Path CONFIG_FILE = Paths.get("config", "asyncparticles.properties");
	public static int limit = 32768;
	public static int renderFailurePerSecondThreshold = 20;
	public static int tickFailurePerSecondThreshold = 5;
	private static boolean asyncClientBlockEntityTick = true;
	private static boolean greedyAsyncClientBlockEntityTick = false;
	private static boolean asyncClientBlockEntityAnimate = true;
	private static boolean forceDoneBlockAnimateTick = false;
	private static boolean forceDoneParticleTick = false;
	private static boolean forceDoneTextureTick = false;
	private static boolean markSyncIfTickFailed = false;
	private static boolean particleLightCache = true;
	private static boolean suppressCME = false;
	private static boolean renderAsync = true;
	private static boolean tickAsync = true;
	private static boolean collideWithCreateModContraptions = true;
	private static boolean collideWithVSModShips = true;
	private static boolean cullParticles = true;
	private static boolean shouldSave;

	public static void load() throws IOException {
		Properties properties = new Properties();
		if (!Files.exists(CONFIG_FILE)) {
			Files.createDirectories(CONFIG_FILE.getParent());
			Files.createFile(CONFIG_FILE);
		} else {
			properties.load(Files.newInputStream(CONFIG_FILE));
			String verStr = properties.getProperty("version_doNotModify");
			int ver = -1;
			try {
				ver = Integer.parseInt(verStr);
			} catch (NumberFormatException ignored) {
			}
			if (ver < 0) {
				properties.setProperty("version_doNotModify", "0");
				asyncClientBlockEntityTick = false;
				properties.setProperty("asyncClientBlockEntityTick", "false");
				shouldSave = true;
			}
		}

		limit = getInt(properties, "limit", 32768);
		renderFailurePerSecondThreshold = getInt(properties, "renderFailurePerSecondThreshold", 20);
		tickFailurePerSecondThreshold = getInt(properties, "tickFailurePerSecondThreshold", 5);

		asyncClientBlockEntityTick = getBoolean(properties, "asyncClientBlockEntityTick", true);
		greedyAsyncClientBlockEntityTick = getBoolean(properties, "greedyAsyncClientBlockEntityTick", false);
		asyncClientBlockEntityAnimate = getBoolean(properties, "asyncClientBlockEntityAnimate", true);
		forceDoneBlockAnimateTick = getBoolean(properties, "forceDoneBlockAnimateTick", false);
		forceDoneParticleTick = getBoolean(properties, "forceDoneParticleTick", false);
		forceDoneTextureTick = getBoolean(properties, "forceDoneTextureTick", false);
		markSyncIfTickFailed = getBoolean(properties, "markSyncIfTickFailed", false);
		particleLightCache = getBoolean(properties, "particleLightCache", true);
		suppressCME = getBoolean(properties, "suppressCME", false);
		renderAsync = getBoolean(properties, "renderAsync", true);
		tickAsync = getBoolean(properties, "tickAsync", true);
		cullParticles = getBoolean(properties, "cullParticles", true);

		if (shouldSave) {
			properties.store(Files.newOutputStream(CONFIG_FILE), null);
			shouldSave = false;
		}
	}

	private static int getInt(Properties properties, String key, int defaultValue) {
		String i = properties.getProperty(key);
		try {
			return Integer.parseInt(i);
		} catch (NumberFormatException e) {
			properties.setProperty(key, String.valueOf(defaultValue));
			shouldSave = true;
			return defaultValue;
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

	public static boolean asyncBlockEntityTick() {
		return asyncClientBlockEntityTick;
	}

	public static boolean greedyAsyncClientBlockEntityTick() {
		return greedyAsyncClientBlockEntityTick;
	}

	public static boolean asyncBlockEntityAnimate() {
		return asyncClientBlockEntityAnimate;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return forceDoneBlockAnimateTick;
	}

	public static boolean forceDoneParticleTick() {
		return forceDoneParticleTick;
	}

	public static boolean forceDoneTextureTick() {
		return forceDoneTextureTick;
	}

	public static boolean markSyncIfTickFailed() {
		return markSyncIfTickFailed;
	}

	public static boolean particleLightCache() {
		return particleLightCache;
	}

	public static boolean suppressCME() {
		return suppressCME;
	}

	public static boolean isRenderAsync() {
		return renderAsync;
	}

	public static boolean isTickAsync() {
		return tickAsync;
	}

	public static boolean isCullParticles() {
		return cullParticles;
	}
}
