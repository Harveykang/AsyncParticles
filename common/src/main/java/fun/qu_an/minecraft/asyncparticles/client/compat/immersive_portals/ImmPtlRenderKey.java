package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchKey;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;

/**
 * A contiguous GPU draw range belongs to both a render layer and a level.
 */
public record ImmPtlRenderKey(
	@Override ParticleRenderType layer,
	@Override ClientLevel level
) implements IParticleBatchKey {
}
