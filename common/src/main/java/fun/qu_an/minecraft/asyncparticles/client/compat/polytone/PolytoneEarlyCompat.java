package fun.qu_an.minecraft.asyncparticles.client.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

public class PolytoneEarlyCompat {
	public static boolean isAvailable() {
		return ModListHelper.POLYTONE_LATER_THAN_5 && MixinConfigHelper.isPolytoneCompatEnabled();
	}
}
