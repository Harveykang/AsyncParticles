package fun.qu_an.minecraft.asyncparticles.client.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;


public class ConfigHelper {
	public static void load() throws Exception {
//		AsyncParticlesConfig.load();
	}

	public static boolean asyncBlockEntityAnimate() {
		return true;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return false;
	}

	public static boolean markSyncIfTickFailed() {
//		TODO: return AsyncParticlesConfig.tick$failBehavior == FailBehavior.MARK_AS_SYNC;
		return false;
	}

	public static boolean isParticleLightCache() {
		return true;
	}

	public static boolean suppressCME() {
		return false;
	}

	public static boolean isTickAsync() {
		return true;
	}

	public static boolean forceDoneParticleTick() {
		return false;
	}

	public static boolean fixParticleLightOnVsShips() {
		return true;
	}

	public static int getParticleLimit() {
		return 16384;
	}

	public static boolean alwaysSpawnRainParticlesOnVsShips() {
		return false;
	}

	public static boolean doCreateRainEffectsIfMoving() {
		return true;
	}

	public static int getRenderFailurePerSecondThreshold() {
		return 20;
	}

	public static int getTickFailurePerSecondThreshold() {
		return 5;
	}

	public static boolean isRenderAsync() {
		return true;
	}

	public static boolean isCompatibilityRendering() {
		return false;
	}

	public static boolean isDelayedRendering() {
		return true;
	}

	public static boolean isCullWeathers() {
		return true;
	}

	// TODO: implement weather particle config, which will not be spawn into physics structures
	public static Set<ResourceLocation> getWeatherParticles() {
		return Set.of();
	}

	public static boolean isCullUnderwaterParticleType() {
		return true;
	}

	public static boolean isRemoveIfMissedTick() {
		return false;
	}

	public static RenderingMode getParticleRenderingMode() {
		return RenderingMode.DELAYED;
	}

	public static boolean isTickWeatherAsync() {
		return true;
	}

	public static boolean isRenderWeatherAsync() {
		return true;
	}

	public static ParticleCullingMode getParticleCullingMode() {
		return ParticleCullingMode.SPHERE;
	}
}
