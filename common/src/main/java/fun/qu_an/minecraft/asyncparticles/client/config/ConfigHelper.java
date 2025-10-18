package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

public class ConfigHelper {
	public static void load() throws Exception {
		AsyncParticlesConfig.load();
	}

	public static boolean asyncBlockEntityAnimate() {
		return tick$animationTickMode != TickMode.SYNCHRONOUSLY;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return tick$animationTickMode == TickMode.FORCE_COMPLETE;
	}

	public static boolean markSyncIfTickFailed() {
//		TODO: return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
		return false;
	}

	public static boolean isParticleLightCache() {
		return particle$particleLightCache;
	}

	public static boolean suppressCME() {
		return tick$suppressCME;
	}

	public static boolean isTickAsync() {
		return tick$particleTickMode != TickMode.SYNCHRONOUSLY;
	}

	public static boolean forceDoneParticleTick() {
		return tick$particleTickMode == TickMode.FORCE_COMPLETE;
	}

	public static boolean fixParticleLightOnVsShips() {
		return valkyrienSkies$fixParticleLights;
	}

	public static int getParticleLimit() {
		return particle$particleLimit;
	}

	public static boolean alwaysSpawnRainParticlesOnVsShips() {
		return valkyrienSkies$rainEffect == RainEffect.ALWAYS;
	}

	public static boolean doCreateRainEffectsIfMoving() {
		return true;
	}

	public static int getRenderFailurePerSecondThreshold() {
		return rendering$failPerSecLimit;
	}

	public static int getTickFailurePerSecondThreshold() {
		return tick$failPerSecLimit;
	}

	public static boolean isRenderAsync() {
		return rendering$particleRenderingMode != RenderingMode.SYNCHRONOUSLY;
	}

	public static boolean isCompatibilityRendering() {
		return rendering$particleRenderingMode == RenderingMode.COMPATIBILITY;
	}

	public static boolean isDelayedRendering() {
		return rendering$particleRenderingMode == RenderingMode.DELAYED;
	}

	public static boolean isCullWeathers() {
		return rendering$cullWeathers;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}

	public static boolean isCullUnderwaterParticleType() {
		return particle$cullUnderwaterParticleType;
	}

	public static boolean isRemoveIfMissedTick() {
		return particle$removeIfMissedTick;
	}

	public static RenderingMode getParticleRenderingMode() {
		return rendering$particleRenderingMode;
	}

	public static boolean isTickWeatherAsync() {
		return tick$tickWeatherAsync;
	}

	public static boolean isRenderWeatherAsync() {
		return rendering$renderWeatherAsync;
	}

	public static boolean isDeferredTextureTick() {
		return tick$deferredTextureTick;
	}

	public static ParticleCullingMode getParticleCullingMode() {
		return rendering$particleCulling;
	}
}
