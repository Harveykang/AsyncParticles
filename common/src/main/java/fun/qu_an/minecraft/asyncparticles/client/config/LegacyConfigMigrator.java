package fun.qu_an.minecraft.asyncparticles.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

class LegacyConfigMigrator {
	static boolean migrate() {
		Path legacyConfigFile = Paths.get("config", "asyncparticles.properties");
		if (!Files.exists(legacyConfigFile)) {
			return false;
		}

		Properties properties = new Properties();
		try (InputStream is = Files.newInputStream(legacyConfigFile)) {
			properties.load(is);
		} catch (IOException e) {
			return false;
		}
		if (properties.get("migrated") != null) {
			return false;
		}
		AsyncParticlesConfig.ConfigObj defaultConfig = new AsyncParticlesConfig.ConfigObj();

		particle$particleLimit = getInt(properties, "limit", defaultConfig.particle.particleLimit);
		if (particle$particleLimit == 32768 || particle$particleLimit < 1024 || particle$particleLimit > 262144) {
			particle$particleLimit = defaultConfig.particle.particleLimit;
		}
		particle$particleLightCache = getBoolean(properties, "particleLightCache", defaultConfig.particle.particleLightCache);

		tick$failPerSecLimit = getInt(properties, "tickFailurePerSecondThreshold", defaultConfig.tick.failPerSecLimit);
		if (tick$failPerSecLimit < 0 || tick$failPerSecLimit > 256) {
			tick$failPerSecLimit = defaultConfig.tick.failPerSecLimit;
		}
		tick$suppressCME = getBoolean(properties, "suppressCME", defaultConfig.tick.suppressCME);
		if (!getBoolean(properties, "asyncClientBlockEntityAnimate",
			defaultConfig.tick.animationTickMode != TickMode.SYNCHRONOUSLY)) {
			tick$animationTickMode = TickMode.SYNCHRONOUSLY;
		} else if (getBoolean(properties, "forceDoneBlockAnimateTick",
			defaultConfig.tick.animationTickMode == TickMode.FORCE_COMPLETE)) {
			tick$animationTickMode = TickMode.FORCE_COMPLETE;
		} else {
			tick$animationTickMode = TickMode.INTERRUPTIBLE;
		}
		if (getBoolean(properties, "forceDoneParticleTick",
			defaultConfig.tick.particleTickMode == TickMode.FORCE_COMPLETE)) {
			tick$particleTickMode = TickMode.FORCE_COMPLETE;
		} else {
			tick$particleTickMode = defaultConfig.tick.particleTickMode;
		}

		rendering$failPerSecLimit = getInt(properties, "renderFailurePerSecondThreshold", defaultConfig.rendering.failPerSecLimit);
		if (rendering$failPerSecLimit < 0 || rendering$failPerSecLimit > 256) {
			rendering$failPerSecLimit = defaultConfig.rendering.failPerSecLimit;
		}
		valkyrienSkies$fixParticleLights = getBoolean(properties, "fixParticleLightOnVsShips", defaultConfig.valkyrienSkies.fixParticleLights);
		valkyrienSkies$rainEffect = getBoolean(properties, "doVsShipRainEffectsIfMoving",
			defaultConfig.valkyrienSkies.rainEffect == RainEffect.ALWAYS)
			? RainEffect.ALWAYS : RainEffect.STATIONARY;

		properties.setProperty("migrated", "");
		try (OutputStream os = Files.newOutputStream(legacyConfigFile)) {
			properties.store(os, null);
		} catch (IOException e) {
			return true;
		}
		return true;
	}

	private static int getInt(Properties properties, String key, int defaultValue) {
		String i = properties.getProperty(key);
		try {
			return Integer.parseInt(i);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
		String b = properties.getProperty(key);
		if (b != null) {
			// !Boolean.toString(!defaultValue).equalsIgnoreCase(b) ? defaultValue : !defaultValue;
			return Boolean.toString(!defaultValue).equalsIgnoreCase(b) != defaultValue;
		} else {
			return defaultValue;
		}
	}
}
