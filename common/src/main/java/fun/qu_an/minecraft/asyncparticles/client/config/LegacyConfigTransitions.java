package fun.qu_an.minecraft.asyncparticles.client.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

public class LegacyConfigTransitions {
	public static boolean migrate() {
		Path legacyConfigFile = Paths.get("config", "asyncparticles.properties");
		if (!Files.exists(legacyConfigFile)) {
			return false;
		}

		Properties properties = new Properties();
		AsyncParticlesConfig.ConfigObj defaultConfig = new AsyncParticlesConfig.ConfigObj();

		particle$particleLimit = getInt(properties, "limit", defaultConfig.particle.particleLimit);
		particle$particleLightCache = getBoolean(properties, "particleLightCache", defaultConfig.particle.particleLightCache);

		tick$failPerSecLimit = getInt(properties, "tickFailurePerSecondThreshold", defaultConfig.tick.failPerSecLimit);
		tick$suppressCME = getBoolean(properties, "suppressCME", defaultConfig.tick.suppressCME);
		if (!getBoolean(properties, "asyncClientBlockEntityAnimate",
			defaultConfig.tick.asyncAnimationTickBehavior != AsyncTickBehavior.DISABLED)) {
			tick$asyncAnimationTickBehavior = AsyncTickBehavior.DISABLED;
		} else if (getBoolean(properties, "forceDoneBlockAnimateTick",
			defaultConfig.tick.asyncAnimationTickBehavior == AsyncTickBehavior.FORCE_COMPLETE)) {
			tick$asyncAnimationTickBehavior = AsyncTickBehavior.FORCE_COMPLETE;
		} else {
			tick$asyncAnimationTickBehavior = AsyncTickBehavior.INTERRUPTIBLE;
		}
		if (getBoolean(properties, "forceDoneParticleTick",
			defaultConfig.tick.asyncParticleTickBehavior == AsyncTickBehavior.FORCE_COMPLETE)) {
			tick$asyncParticleTickBehavior = AsyncTickBehavior.FORCE_COMPLETE;
		} else {
			tick$asyncParticleTickBehavior = defaultConfig.tick.asyncParticleTickBehavior;
		}

		rendering$failPerSecLimit = getInt(properties, "renderFailurePerSecondThreshold", defaultConfig.rendering.failPerSecLimit);

		valkyrienSkies$fixParticleLights = getBoolean(properties, "fixParticleLightOnVsShips", defaultConfig.valkyrienSkies.fixParticleLights);
		valkyrienSkies$rainEffect = getBoolean(properties, "doVsShipRainEffectsIfMoving",
			defaultConfig.valkyrienSkies.rainEffect == RainEffect.ALWAYS)
			? RainEffect.ALWAYS : RainEffect.STATIONARY;

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
