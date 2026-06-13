package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig.*;

public class ConfigHelper {
	public static boolean isAsyncAnimateTick() {
		return tick$asyncAnimateTick;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return tick$asyncAnimateTick;
	}

	public static boolean particleLightCache() {
		return particle$particleLightCache;
	}

	public static boolean suppressCME() {
		return tick$suppressCME;
	}

	public static boolean isTickAsync() {
		return tick$particleAsyncMode != ParticleAsyncMode.DISABLE;
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

	public static boolean isTickWeatherAsync() {
		return tick$tickWeatherAsync;
	}

	public static boolean isDeferredTextureTick() {
		return tick$deferredTextureTick;
	}

	public static List<? extends Class<?>> getSyncParticleClassesRender() {
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
		return rendering$appendNewParticlesToRenderer && particle$particleLightCache;
	}
}
