package fun.qu_an.minecraft.asyncparticles.client.fabric;

import com.mojang.blaze3d.pipeline.RenderTarget;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.function.Predicate;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void irisOpaque(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");

		LevelRenderer levelRenderer = mc.levelRenderer;

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		AsyncRenderer.renderAsync = ConfigHelper.isRenderAsync();
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;
	}

	public static void irisTranslucent(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;

		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		AsyncRenderer.renderAsync = ConfigHelper.isRenderAsync();
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}
}
