package fun.qu_an.minecraft.asyncparticles.client.compat.moreculling;

import ca.fxco.moreculling.api.config.ConfigAdditions;

public class MoreCullingCompat {
	public static void init() {
		ConfigAdditions.disableOption(
			"moreculling.config.option.rainCulling",
			"Redirected by AsyncParticles mod.",
			() -> false
		);
	}
}
