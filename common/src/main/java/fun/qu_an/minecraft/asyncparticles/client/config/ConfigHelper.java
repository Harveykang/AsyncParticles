package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class ConfigHelper {
	public static void load() throws Exception {
		AsyncParticlesConfig.reload();
	}

	public static boolean asyncBlockEntityAnimate() {
		return AsyncParticlesConfig.tick$asyncAnimationTickBehavior != AsyncTickBehavior.DISABLED;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return AsyncParticlesConfig.tick$asyncAnimationTickBehavior == AsyncTickBehavior.FORCE_COMPLETE;
	}

	public static boolean markSyncIfTickFailed() {
//		TODO: return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
		return false;
	}

	public static boolean particleLightCache() {
		return AsyncParticlesConfig.tick$particleLightCache;
	}

	public static boolean suppressCME() {
		return AsyncParticlesConfig.tick$suppressCME;
	}

	public static boolean isTickAsync() {
		return AsyncParticlesConfig.tick$asyncParticleTickBehavior != AsyncTickBehavior.DISABLED;
	}

	public static boolean forceDoneParticleTick() {
		return AsyncParticlesConfig.tick$asyncParticleTickBehavior == AsyncTickBehavior.FORCE_COMPLETE;
	}

	public static boolean fixParticleLightOnVsShips() {
		return AsyncParticlesConfig.valkyrienSkies$fixParticleLights;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}

	public static int getLimit() {
		return AsyncParticlesConfig.tick$particleLimit;
	}

	public static boolean doVsShipRainEffectsIfMoving() {
		return AsyncParticlesConfig.valkyrienSkies$rainEffect == RainEffect.STATIONARY;
	}

	public static boolean doCreateRainEffectsIfMoving() {
		return true;
	}

	public static int getRenderFailurePerSecondThreshold() {
		return AsyncParticlesConfig.rendering$failPerSecLimit;
	}

	public static int getTickFailurePerSecondThreshold() {
		return AsyncParticlesConfig.tick$failPerSecLimit;
	}

	public static boolean isRenderAsync() {
		return AsyncParticlesConfig.rendering$asyncParticleRendering;
	}
}
