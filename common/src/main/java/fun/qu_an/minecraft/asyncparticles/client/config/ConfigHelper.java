package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

public class ConfigHelper {
	public static boolean isAsyncAnimateTick() {
		return tick$animationTickMode && !Diagnostic.isTemporaryDisableAnimationTick();
	}

	public static boolean particleLightCache() {
		return particle$particleLightCache;
	}

	public static boolean suppressCME() {
		return tick$suppressCME;
	}

	public static boolean isAsyncParticleTick() {
		return tick$particleAsyncMode != ParticleAsyncMode.DISABLE && !Diagnostic.isTemporaryDisableAsyncParticleTick();
	}

	public static boolean isSplitParticleTick() {
		return tick$particleAsyncMode == ParticleAsyncMode.SPLIT;
	}

	public static int getParticleLimit() {
		return particle$particleLimit;
	}

	public static int getRenderFailurePerSecondThreshold() {
		return rendering$failPerSecLimit;
	}

	public static int getTickFailurePerSecondThreshold() {
		return tick$failPerSecLimit;
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

	public static boolean isCullWeathers() {
		return rendering$cullWeathers;
	}

	public static boolean isTickWeatherAsync() {
		return tick$tickWeatherAsync && !Diagnostic.isTemporaryDisableAsyncRainTick();
	}

	public static boolean isDeferredTextureTick() {
		return tick$deferredTextureTick;
	}

	public static ParticleCullingMode getParticleCullingMode() {
		return rendering$particleCulling;
	}

	public static List<? extends Class<?>> getRenderSyncParticleClasses() {
		return rendering$syncParticleClasses
			.stream()
			.map(className -> {
				try {
					return Class.forName(className);
				} catch (ClassNotFoundException e) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}

	public static List<? extends Class<?>> getSyncParticleClassesTick() {
		return tick$syncParticleClasses
			.stream()
			.map(className -> {
				try {
					return Class.forName(className);
				} catch (ClassNotFoundException e) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}

	public static boolean isGpuParticles() {
		return rendering$gpuAcceleration;
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

	public static boolean isGpuOnlyAsyncParticleTick() {
		return tick$gpuOnlyAsyncParticleTick || Diagnostic.isTemporaryGpuOnlyAsyncParticleTick();
	}

	public static boolean mobileCompatMultiDraw() {
		return mobile$multiDrawWorkaround;
	}

	public static ComputeExecutionStage getComputeExecutionStage() {
		return rendering$computeExecutionStage;
	}

	public static ParticleCleanupStrategy getParticleCleanupStrategy() {
		return particle$cleanupStrategy;
	}

	public static RainEffect getCreateRainEffect() {
		return create$rainEffect;
	}

	public static int getTickRainBlockingRange() {
		return create$tickRainBlockingRange;
	}

	public static RainEffect getVSRainEffect() {
		return AsyncParticlesConfig.valkyrienSkies$rainEffect;
	}

	public static boolean fixParticleLightOnSableSublevel() {
		return sable$fixParticleLights;
	}

	public static boolean fixParticleLightOnVsShips() {
		return valkyrienSkies$fixParticleLights;
	}
}
