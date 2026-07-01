package fun.qu_an.minecraft.asyncparticles.client.compat.particle_core;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import me.fzzyhmstrs.particle_core.PcConfig;
import net.minecraft.world.phys.Vec3;

public class ParticleCoreCompat {
	public static boolean shouldRenderParticle(GpuParticleAddon gpuParticle, Vec3 cameraPosition) {
		return PcConfig.INSTANCE.shouldRenderParticle(gpuParticle.asyncparticles$getX(), gpuParticle.asyncparticles$getY(), gpuParticle.asyncparticles$getZ(), cameraPosition);
	}
}
