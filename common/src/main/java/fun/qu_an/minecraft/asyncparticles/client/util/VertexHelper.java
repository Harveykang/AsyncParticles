
package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import org.slf4j.Logger;

public class VertexHelper {
	private static boolean warnedNegativeAlpha = false;
	private static final Logger LOGGER = LogUtils.getLogger();

	public static VertexConsumer setColor(VertexConsumer vertexConsumer, float red, float green, float blue, float alpha) {
		if (alpha < 0f) {
			if (!warnedNegativeAlpha) {
				LOGGER.warn("Negative alpha value {} detected. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
				warnedNegativeAlpha = true;
			}
			if ((AsyncRenderBehavior.getInstance().isParticlePhase() && ThreadUtil.isOnRenderThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				alpha = 0f;
			}
		}
		return vertexConsumer.setColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}
}
