package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;

public class ImmersivePortalsCompat {
	public static ClientLevel getParticleWorld(TextureSheetParticle particle) {
		return ((IEParticle) particle).portal_getWorld();
	}
}
