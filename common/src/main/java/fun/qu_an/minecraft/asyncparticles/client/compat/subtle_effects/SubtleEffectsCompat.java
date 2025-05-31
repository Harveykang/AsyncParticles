package fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects;

import einstein.subtle_effects.init.ModConfigs;
import einstein.subtle_effects.util.ParticleAccessor;
import einstein.subtle_effects.util.Util;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class SubtleEffectsCompat {
	public static boolean shouldRenderParticle(ParticleRenderType renderType, Particle particle, Camera camera, ClientLevel level) {
		if (renderType == ParticleRenderType.CUSTOM) {
			return true;
		}

		ParticleAccessor accessor = (ParticleAccessor) particle;
		if (!ModListHelper.VS_LOADED &&
			ModConfigs.GENERAL.cullParticlesInUnloadedChunks &&
			!Util.isChunkLoaded(level, particle.x, particle.z)) {
			return false;
		}

		int distance = ModConfigs.GENERAL.particleRenderDistance << 4;
		return accessor.subtleEffects$wasForced() ||
			   camera.getPosition().distanceToSqr(accessor.getX(), accessor.getY(), accessor.getZ()) <
			   (double) (distance * distance);
	}
}
