package fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_SUBTLE_EFFECTS_LATER_THAN_1_12;

public class SubtleEffectsCompat {
	public static boolean shouldRenderParticle(Particle instance, Camera camera, ParticleRenderType renderType) {
		if (IS_SUBTLE_EFFECTS_LATER_THAN_1_12) {
			return fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.v1_12.SubtleEffectsCompat
				.shouldRenderParticle(instance, camera, renderType);
		} else {
			return fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.v1_10.SubtleEffectsCompat
				.shouldRenderParticle(instance, camera, renderType);
		}
	}
}
