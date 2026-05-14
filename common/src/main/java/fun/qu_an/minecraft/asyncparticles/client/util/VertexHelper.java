package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;

public class VertexHelper {
	private static boolean warnedNegativeAlpha = false;

	public static float checkAlpha(float alpha) {
		if (alpha < 0f) {
			if (!warnedNegativeAlpha) {
				LogUtils.getLogger().warn("Negative alpha value {} detected. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
				warnedNegativeAlpha = true;
			}
			if ((AsyncRenderBehavior.INSTANCE.isParticlePhase() && ThreadUtil.isOnRenderThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				alpha = 0f;
			}
		}
		return alpha;
	}
}
