package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Set;

public class SimplePropertiesConfig {
	@Deprecated(forRemoval = true)
	public static void load() throws IOException {
		AsyncParticlesConfig.reload();
	}

	@Deprecated(forRemoval = true)
	public static boolean asyncBlockEntityAnimate() {
		return AsyncParticlesConfig.tick$asyncAnimationTickBehavior != AsyncTickBehavior.DISABLED;
	}

	@Deprecated(forRemoval = true)
	public static boolean forceDoneBlockAnimateTick() {
		return AsyncParticlesConfig.tick$asyncAnimationTickBehavior == AsyncTickBehavior.FORCE_COMPLETE;
	}

	@Deprecated(forRemoval = true)
	public static boolean markSyncIfTickFailed() {
		return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
	}

	@Deprecated(forRemoval = true)
	public static boolean particleLightCache() {
		return AsyncParticlesConfig.tick$particleLightCache;
	}

	@Deprecated(forRemoval = true)
	public static boolean suppressCME() {
		return AsyncParticlesConfig.tick$suppressCME;
	}

	@Deprecated(forRemoval = true)
	public static boolean isTickAsync() {
		return AsyncParticlesConfig.tick$asyncParticleTickBehavior != AsyncTickBehavior.DISABLED;
	}

	@Deprecated(forRemoval = true)
	public static boolean forceDoneParticleTick() {
		return AsyncParticlesConfig.tick$asyncParticleTickBehavior == AsyncTickBehavior.FORCE_COMPLETE;
	}

	@Deprecated(forRemoval = true)
	public static boolean fixParticleLightOnVsShips() {
		return AsyncParticlesConfig.valkyrienSkies$fixParticleLights;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	@Deprecated(forRemoval = true)
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}

	@Deprecated(forRemoval = true)
	public static int getLimit() {
		return AsyncParticlesConfig.tick$particleLimit;
	}

	@Deprecated(forRemoval = true)
	public static boolean doVsShipRainEffectsIfMoving() {
		return AsyncParticlesConfig.valkyrienSkies$rainEffect == RainEffect.STATIONARY;
	}

	@Deprecated(forRemoval = true)
	public static boolean doCreateRainEffectsIfMoving() {
		return true;
	}

	@Deprecated(forRemoval = true)
	public static int getRenderFailurePerSecondThreshold() {
		return AsyncParticlesConfig.rendering$failPerSecLimit;
	}

	@Deprecated(forRemoval = true)
	public static int getTickFailurePerSecondThreshold() {
		return AsyncParticlesConfig.tick$failPerSecLimit;
	}
}
