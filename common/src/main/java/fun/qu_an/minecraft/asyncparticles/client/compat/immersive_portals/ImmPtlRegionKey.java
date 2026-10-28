package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchKey;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;

record ImmPtlRegionKey(ParticleRenderType layer, ClientLevel level,
					   long x, long y, long z) implements IParticleBatchKey {
}
