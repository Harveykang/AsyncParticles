package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

public class ConfigHelper {
	public static void load() throws Exception {
		AsyncParticlesConfig.load();
	}

	public static boolean asyncAnimateTick() {
		return tick$animationTickMode != TickMode.SYNCHRONOUSLY;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return tick$animationTickMode == TickMode.FORCE_COMPLETE;
	}

	public static boolean markSyncIfTickFailed() {
//		TODO: return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
		return false;
	}

	public static boolean particleLightCache() {
		return particle$particleLightCache;
	}

	public static int getParticleLimit() {
		return particle$particleLimit;
	}

	public static boolean isCullUnderwaterParticleType() {
		return particle$cullUnderwaterParticleType;
	}

	public static boolean isRemoveIfMissedTick() {
		return particle$removeIfMissedTick;
	}

	public static boolean isParallelQueueRemoval() {
		return particle$parallelQueueRemoval;
	}

	public static boolean isParallelQueueEviction() {
		return particle$parallelQueueEviction;
	}

	public static boolean isAppendNewParticlesToRenderer() {
		return rendering$appendNewParticlesToRenderer;
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

	public static boolean isTickWeatherAsync() {
		return tick$tickWeatherAsync;
	}

	public static int getTickFailurePerSecondThreshold() {
		return tick$failPerSecLimit;
	}

	public static int getRenderFailurePerSecondThreshold() {
		return rendering$failPerSecLimit;
	}

	public static RenderingMode particleRenderingMode() {
		return rendering$particleRenderingMode;
	}

	public static boolean isRenderAsync() {
		return rendering$particleRenderingMode != RenderingMode.SYNCHRONOUSLY;
	}

	public static boolean isCullWeathers() {
		return rendering$cullWeathers;
	}

	public static RenderingMode getParticleRenderingMode() {
		return rendering$particleRenderingMode;
	}

	public static ParticleCullingMode getParticleCullingMode() {
		return rendering$particleCulling;
	}

	public static boolean isGpuParticles() {
		return rendering$gpuAcceleration;
	}

	public static boolean fixParticleLightOnVsShips() {
		return valkyrienSkies$fixParticleLights;
	}

	public static RainEffect getCreateRainEffect() {
		return AsyncParticlesConfig.create$rainEffect;
	}

	public static RainEffect getVSRainEffect() {
		return AsyncParticlesConfig.valkyrienSkies$rainEffect;
	}

	public static boolean alwaysSpawnRainParticlesOnVsShips() {
		return valkyrienSkies$rainEffect == RainEffect.ALWAYS;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}
}
