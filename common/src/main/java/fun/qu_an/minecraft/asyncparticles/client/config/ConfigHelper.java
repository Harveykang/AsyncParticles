package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class ConfigHelper {
	public static void load() throws Exception {
		AsyncParticlesConfig.load();
	}

	public static boolean asyncBlockEntityAnimate() {
		return AsyncParticlesConfig.tick$animationTickMode != TickMode.SYNCHRONOUSLY;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return AsyncParticlesConfig.tick$animationTickMode == TickMode.FORCE_COMPLETE;
	}

	public static boolean markSyncIfTickFailed() {
//		TODO: return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
		return false;
	}

	public static boolean particleLightCache() {
		return AsyncParticlesConfig.particle$particleLightCache;
	}

	public static boolean suppressCME() {
		return AsyncParticlesConfig.tick$suppressCME;
	}

	public static boolean isTickAsync() {
		return AsyncParticlesConfig.tick$particleTickMode != TickMode.SYNCHRONOUSLY;
	}

	public static boolean forceDoneParticleTick() {
		return AsyncParticlesConfig.tick$particleTickMode == TickMode.FORCE_COMPLETE;
	}

	public static boolean fixParticleLightOnVsShips() {
		return AsyncParticlesConfig.valkyrienSkies$fixParticleLights;
	}

	public static int getParticleLimit() {
		return AsyncParticlesConfig.particle$particleLimit;
	}

	public static boolean alwaysSpawnRainParticlesOnVsShips() {
		return AsyncParticlesConfig.valkyrienSkies$rainEffect == RainEffect.ALWAYS;
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
		return AsyncParticlesConfig.rendering$particleRenderingMode != RenderingMode.SYNCHRONOUSLY;
	}

	public static boolean isCompatibilityRendering() {
		return AsyncParticlesConfig.rendering$particleRenderingMode == RenderingMode.COMPATIBILITY;
	}

	public static boolean isDelayedRendering() {
		return AsyncParticlesConfig.rendering$particleRenderingMode == RenderingMode.DELAYED;
	}

	public static boolean isCullParticles() {
		return AsyncParticlesConfig.rendering$cullParticles;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}

	public static boolean cullUnderwaterParticleType() {
		return AsyncParticlesConfig.particle$cullUnderwaterParticleType;
	}
}
