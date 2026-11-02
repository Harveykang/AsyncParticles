package fun.qu_an.minecraft.asyncparticles.client.compat.opallight;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.MixinConfigHelper;

public final class OpalLightEarlyCompat {
	public static final boolean AVAILABLE = isSupportedVersion() && MixinConfigHelper.isOpalLightMixinEnabled();

	public static boolean isSupportedVersion() {
		return ModListHelper.OPAL_LIGHT_LATER_THAN_1_2_3_3;
	}
}
