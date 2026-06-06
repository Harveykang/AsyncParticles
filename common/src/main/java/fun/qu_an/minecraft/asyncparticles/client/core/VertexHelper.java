
package fun.qu_an.minecraft.asyncparticles.client.core;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.slf4j.Logger;

public class VertexHelper {
	private static boolean warnedIllegalAlpha = false;
	private static boolean warnedIllegalColor = false;
	private static final Logger LOGGER = LogUtils.getLogger();

	public static VertexConsumer setColor(VertexConsumer vertexConsumer, float red, float green, float blue, float alpha) {
		if (alpha < 0f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalAlpha) {
					LOGGER.warn("Negative alpha value {} detected. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
					warnedIllegalAlpha = true;
				}
				alpha = 0f;
			}
		} else if (alpha > 1f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalAlpha) {
					LOGGER.warn("Alpha value {} is greater than 1. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
					warnedIllegalAlpha = true;
				}
				alpha = 1f;
			}
		}
		return vertexConsumer.setColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	public static float getAlpha(float alpha) {
		if (alpha < 0f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalAlpha) {
					LOGGER.warn("Negative alpha value {} detected. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
					warnedIllegalAlpha = true;
				}
				alpha = 0f;
			}
		} else if (alpha > 1f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalAlpha) {
					LOGGER.warn("Alpha value {} is greater than 1. This may cause unexpected behavior. You can ignore it if nothing is broken.", alpha, new IllegalStateException(""));
					warnedIllegalAlpha = true;
				}
				alpha = 1f;
			}
		}
		return alpha;
	}

	public static float getColor(float color) {
		if (color < 0f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalColor) {
					LOGGER.warn("Negative color value {} detected. This may cause unexpected behavior. You can ignore it if nothing is broken.", color, new IllegalStateException(""));
					warnedIllegalColor = true;
				}
				color = 0f;
			}
		} else if (color > 1f) {
			if ((AsyncRenderBehavior.isParticlePhase() && ThreadUtil.isOnMainThread()) ||
				ThreadUtil.isOnParticleRendererThread()) {
				if (!warnedIllegalColor) {
					LOGGER.warn("Color value {} is greater than 1. This may cause unexpected behavior. You can ignore it if nothing is broken.", color, new IllegalStateException(""));
					warnedIllegalColor = true;
				}
				color = 1f;
			}
		}
		return color;
	}
}
