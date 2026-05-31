package fun.qu_an.minecraft.asyncparticles.client.core.particle.fix_black_destruction_particle;

import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;

public class TerrainParticleBehavior {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>();
}
