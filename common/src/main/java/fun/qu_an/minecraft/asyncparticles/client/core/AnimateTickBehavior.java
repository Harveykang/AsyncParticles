package fun.qu_an.minecraft.asyncparticles.client.core;

import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;

public class AnimateTickBehavior {
	public static final ParticleThreadLocal<Boolean> CULL_UNDERWATER_PARTICLE_TYPE = ParticleThreadLocal.withInitial(() -> false);
}
