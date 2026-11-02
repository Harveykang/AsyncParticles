package fun.qu_an.minecraft.asyncparticles.client.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

public class PolytoneEarlyCompat {
	public static final boolean AVAILABLE = isSupportedVersion() && MixinConfigHelper.isPolytoneMixinEnabled();

	public static boolean isSupportedVersion() {
		return ModListHelper.POLYTONE_LATER_THAN_5_1;
	}
}
