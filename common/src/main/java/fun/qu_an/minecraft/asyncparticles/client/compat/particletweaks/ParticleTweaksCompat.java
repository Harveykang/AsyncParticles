package fun.qu_an.minecraft.asyncparticles.client.compat.particletweaks;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.lunade.particletweaks.scale.api.ParticleScaleHandler;
import net.lunade.particletweaks.scale.api.ParticleScaler;
import net.lunade.particletweaks.scale.impl.ParticleScaleInterface;
import net.minecraft.client.particle.SingleQuadParticle;

public class ParticleTweaksCompat {
	public static SingleQuadParticle.Layer modifyLayer(GpuParticleAddon particle, SingleQuadParticle.Layer original) {
		if (!(particle instanceof ParticleScaleInterface scaleInterface)) {
			return original;
		}
		ParticleScaleHandler scaleHandler = scaleInterface.particleTweaks$getScaleHandler();
		if (scaleHandler == null) {
			return original;
		}
		if (original != SingleQuadParticle.Layer.OPAQUE && original != SingleQuadParticle.Layer.OPAQUE_TERRAIN) {
			return original;
		}

		ParticleScaler entrance = scaleHandler.entrance();
		if (entrance != null && entrance.isFade()) {
			return original == SingleQuadParticle.Layer.OPAQUE_TERRAIN
				? SingleQuadParticle.Layer.TRANSLUCENT_TERRAIN
				: SingleQuadParticle.Layer.TRANSLUCENT;
		}
		ParticleScaler exit = scaleHandler.exit();
		if (exit != null && exit.isFade()) {
			return original == SingleQuadParticle.Layer.OPAQUE_TERRAIN
				? SingleQuadParticle.Layer.TRANSLUCENT_TERRAIN
				: SingleQuadParticle.Layer.TRANSLUCENT;
		}

		return original;
	}

	public static float modifyQuadSize(GpuParticleAddon particle, float partialTickTime, float call) {
		if (!(particle instanceof ParticleScaleInterface scaleInterface)) {
			return call;
		}
		ParticleScaleHandler scaleHandler = scaleInterface.particleTweaks$getScaleHandler();
		if (scaleHandler == null) {
			return call;
		}

		ParticleScaler entrance = scaleHandler.entrance();
		ParticleScaler exit = scaleHandler.exit();
		if (entrance != null && entrance.isSize()) {
			call *= entrance.getScale(partialTickTime);
		}
		if (exit != null && exit.isSize()) {
			call *= exit.getScale(partialTickTime);
		}

		return call;
	}

	public static float modifyOColor(GpuParticleAddon particle, float alpha) {
		if (!(particle instanceof ParticleScaleInterface scaleInterface)) {
			return alpha;
		}
		ParticleScaleHandler scaleHandler = scaleInterface.particleTweaks$getScaleHandler();
		if (scaleHandler == null) {
			return alpha;
		}

		ParticleScaler entrance = scaleHandler.entrance();
		ParticleScaler exit = scaleHandler.exit();
		if (entrance != null && entrance.isFade()) {
			alpha *= entrance.getScale(0);
		}
		if (exit != null && exit.isFade()) {
			alpha *= exit.getScale(0);
		}

		return alpha;
	}

	public static int modifyColor(GpuParticleAddon particle, int original) {
		if (!(particle instanceof ParticleScaleInterface scaleInterface)) {
			return original;
		}
		ParticleScaleHandler scaleHandler = scaleInterface.particleTweaks$getScaleHandler();
		if (scaleHandler == null) {
			return original;
		}

		float alpha = particle.asyncparticles$getAlpha();
		ParticleScaler entrance = scaleHandler.entrance();
		ParticleScaler exit = scaleHandler.exit();
		if (entrance != null && entrance.isFade()) {
			alpha *= entrance.getScale(1);
		}
		if (exit != null && exit.isFade()) {
			alpha *= exit.getScale(1);
		}

		return (int) (alpha * 255f) << 24 | (original & 0xFFFFFF);
	}
}
