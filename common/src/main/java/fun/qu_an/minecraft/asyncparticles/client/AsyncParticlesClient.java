package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.moreculling.MoreCullingCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

@Environment(EnvType.CLIENT)
public class AsyncParticlesClient {
	public static final String MOD_ID = "asyncparticles";
	public static final String ISSUE_URL = "https://github.com/Harveykang/AsyncParticles/issues";

	public static void init() {
		if (!IS_CLIENT) {
			return;
		}
		if (MORE_CULLING_LOADED) {
			MoreCullingCompat.init();
		}
		if (PARTICLERAIN_LOADED) {
			ParticleRainCompat.init();
		}
	}
}
