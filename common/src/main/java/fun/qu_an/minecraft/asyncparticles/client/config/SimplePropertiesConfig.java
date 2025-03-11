package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SimplePropertiesConfig {
	public static final Path CONFIG_FILE = Paths.get("config", "asyncparticles.properties");
	public static final int DEFAULT_LIMIT = 32768;
	public static int limit = DEFAULT_LIMIT;
	private static boolean asyncClientBlockEntityTick = true;
	private static boolean greedyAsyncClientBlockEntityTick = false;
	private static boolean asyncClientBlockEntityAnimate = true;
	private static boolean forceSyncLevelRenderMarkDirty = false;
	private static boolean forceDoneBlockAnimateTick = false;
	private static boolean forceDoneParticleTick = false;
	private static boolean forceDoneTextureTick = false;
	private static boolean markSyncIfTickFailed = false;
	private static boolean ignoreParticleTickExceptions = false;
	private static boolean particleLightCache = true;
	private static boolean collideWithCreateModContraptions = true;
	private static boolean collideWithVSModShips = true;

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
		particleLightCache = getBoolean(properties, "particleLightCache", true);

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

	public static boolean asyncBlockEntityTick() {
		return asyncClientBlockEntityTick;
	}

	public static boolean greedyAsyncClientBlockEntityTick() {
		return greedyAsyncClientBlockEntityTick;
	}

	public static boolean asyncBlockEntityAnimate() {
		return !ModListHelper.PHYSICSMOD_LOADED && asyncClientBlockEntityAnimate;
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

	public static boolean ignoreParticleTickExceptions() {
		return ignoreParticleTickExceptions;
	}

	public static boolean particleLightCache() {
		return particleLightCache;
	}

	public static boolean forceSyncLevelRendererMarkDirty() {
		return ModListHelper.SODIUM_LOADED // can't mark dirty asynchronously in sodium
			   || forceSyncLevelRenderMarkDirty;
	}
}
