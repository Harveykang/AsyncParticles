package fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects;

import einstein.subtle_effects.init.ModConfigs;
import einstein.subtle_effects.util.ParticleAccessor;
import einstein.subtle_effects.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public class SubtleEffectsCompat {
	public static boolean shouldRenderParticle(Particle particle, Camera camera) {
		if (!ModConfigs.GENERAL.enableParticleCulling) {
			return true;
		}

		ParticleAccessor accessor = (ParticleAccessor)particle;
		if (ModConfigs.GENERAL.cullParticlesInUnloadedChunks &&
			!Util.isChunkLoaded(particle.level, accessor.getX(), accessor.getZ())) {
			return false;
		}

		int distance = ModConfigs.GENERAL.particleRenderDistance << 4;
		return accessor.subtleEffects$wasForced() ||
			   camera.getPosition().distanceToSqr(accessor.getX(), accessor.getY(), accessor.getZ()) < distance * distance;

	}
}
