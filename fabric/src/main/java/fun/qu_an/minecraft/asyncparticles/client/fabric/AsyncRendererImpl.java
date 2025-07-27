package fun.qu_an.minecraft.asyncparticles.client.fabric;

import com.mojang.blaze3d.pipeline.RenderTarget;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;
	}

	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");
		LevelRenderer levelRenderer = mc.levelRenderer;

		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}
}
