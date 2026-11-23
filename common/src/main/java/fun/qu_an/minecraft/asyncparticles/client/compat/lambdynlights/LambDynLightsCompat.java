package fun.qu_an.minecraft.asyncparticles.client.compat.lambdynlights;

import dev.lambdaurora.lambdynlights.DynamicLightsMode;
import dev.lambdaurora.lambdynlights.LambDynLights;
import net.minecraft.core.BlockPos;

public class LambDynLightsCompat {
	public static int addDynamicLight(BlockPos pos, int light) {
		LambDynLights lamb = LambDynLights.get();
		if (lamb.config.getDynamicLightsMode() == DynamicLightsMode.OFF) {
			return light;
		}
		return lamb.getLightmapWithDynamicLight(lamb.getDynamicLightLevel(pos), light);
	}
}
