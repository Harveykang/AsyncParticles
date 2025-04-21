package fun.qu_an.minecraft.asyncparticles.client.neoforge;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.function.Predicate;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void irisOpaque(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
		if (!AsyncRenderer.isMixedParticleRenderingSetting()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");

		profiler.push("wait_for_async_tasks");
		AsyncRenderer.waitForAsyncTasks();
		profiler.pop();

		ParticleEngine particleEngine = mc.particleEngine;
		particleEngine.render(lightTexture, camera, f, null, predicate);
	}

	public static void irisTranslucent(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
		if (!AsyncRenderer.isMixedParticleRenderingSetting()) {
			return;
		}
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
		particleEngine.render(lightTexture, camera, f, null, predicate);
		// reset blend func and culling state
		// other mods may change them...
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}
}
