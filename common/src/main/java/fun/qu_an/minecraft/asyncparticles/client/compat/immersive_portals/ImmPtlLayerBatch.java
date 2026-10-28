package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchKey;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import net.minecraft.world.phys.Vec3;

public final class ImmPtlLayerBatch extends LayerBatch {
	public final Vec3 origin;
	public int groupOffset;
	public int groupCount;

	public ImmPtlLayerBatch(IParticleBatchKey key, Vec3 origin) {
		super(key);
		this.origin = origin;
	}
}
