package fun.qu_an.minecraft.asyncparticles.client.neoforge;

import com.mojang.blaze3d.pipeline.RenderTarget;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void endOpaque(float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderer.renderAsync = isAsync;
		mc.particleEngine.render(lightTexture, camera, f, null, t -> !t.isTranslucent());
		AsyncRenderer.renderAsync = false;
	}

	public static void endTranslucent(float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");
		LevelRenderer levelRenderer = mc.levelRenderer;

		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

		AsyncRenderer.renderAsync = isAsync;
		mc.particleEngine.render(lightTexture, camera, f, null, ParticleRenderType::isTranslucent);
		AsyncRenderer.renderAsync = false;

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}
}
