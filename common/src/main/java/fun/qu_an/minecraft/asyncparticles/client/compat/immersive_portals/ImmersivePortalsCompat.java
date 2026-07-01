package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.Minecraft;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;

public class ImmersivePortalsCompat {
	public static boolean shouldRenderParticle(GpuParticleAddon gpuParticle) {
		return ((IEParticle) gpuParticle).portal_getWorld() == Minecraft.getInstance().player.level();
	}
}
