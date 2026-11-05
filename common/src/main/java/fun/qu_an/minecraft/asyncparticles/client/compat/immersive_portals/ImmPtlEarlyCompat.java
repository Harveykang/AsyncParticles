package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

public final class ImmPtlEarlyCompat {
	public static final boolean AVAILABLE = ModListHelper.IMMERSIVE_PORTALS_LOADED
		&& MixinConfigHelper.isImmersivePortalsMixinEnabled();
}
