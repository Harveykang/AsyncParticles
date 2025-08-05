package fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.v1_12;

import einstein.subtle_effects.init.ModConfigs;
import einstein.subtle_effects.util.ParticleAccessor;
import einstein.subtle_effects.util.Util;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class SubtleEffectsCompat {
	public static boolean shouldRenderParticle(Particle instance, Camera camera, ParticleRenderType renderType) {
		if (!ModConfigs.GENERAL.enableParticleCulling) {
			return true;
		}

		if (renderType == ParticleRenderType.CUSTOM) {
			return true;
		}

		if (!ModListHelper.VS_LOADED &&
			ModConfigs.GENERAL.cullParticlesInUnloadedChunks &&
			!Util.isChunkLoaded(Minecraft.getInstance().level, instance.x, instance.z)) {
			return false;
		}

		ParticleAccessor accessor = (ParticleAccessor) instance;
		int distance = ModConfigs.GENERAL.particleRenderDistance.get() << 4;
		return accessor.subtleEffects$wasForced() ||
			   camera.getPosition().distanceToSqr(accessor.getX(), accessor.getY(), accessor.getZ()) <
			   (double) (distance * distance);
	}

}
