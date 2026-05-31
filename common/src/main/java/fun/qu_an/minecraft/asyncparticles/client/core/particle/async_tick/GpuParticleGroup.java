package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;

public class GpuParticleGroup extends QuadParticleGroup {
	public GpuParticleGroup(ParticleEngine engine, ParticleRenderType particleType) {
		super(engine, particleType);
	}
}
