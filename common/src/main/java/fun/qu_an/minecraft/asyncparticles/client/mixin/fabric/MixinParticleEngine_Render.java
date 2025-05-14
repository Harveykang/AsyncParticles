package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.CustomTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	public TextureManager textureManager;

	@Shadow
	@Mutable
	public static List<ParticleRenderType> RENDER_ORDER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		boolean renderAsync = AsyncRenderer.isRenderAsync();
		if (renderAsync) {
			profiler.push("wait_for_async_tasks");
			AsyncRenderer.tryWaitForAsyncTasks();
			profiler.pop();
		}

		profiler.push("prepare");
		Frustum frustum = AsyncRenderer.frustum;
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		PoseStack poseStack2 = RenderSystem.getModelViewStack();
		poseStack2.pushPose();
		poseStack2.mulPoseMatrix(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		profiler.pop();

		boolean cullParticles = ConfigHelper.isCullParticles();
		boolean mixedParticleRendering = AsyncRenderer.isMixedParticleRendering();
		for (ParticleRenderType particleRenderType : RENDER_ORDER) {
			// FABRIC skips NO_RENDER
			//				if (particleRenderType == ParticleRenderType.NO_RENDER) {
			//					continue;
			//				}
			Queue<Particle> queue = this.particles.get(particleRenderType);
			if (queue == null || queue.isEmpty()) {
				continue;
			}
			profiler.push("render_sync");
			Collection<? extends Particle> syncParticles;
			Tesselator tesselator;
			BufferBuilder bufferBuilder;
			boolean enableCull;
			BufferBuilder toBegin;
			if (!renderAsync) {
				enableCull = cullParticles;
				syncParticles = queue;
				tesselator = Tesselator.getInstance();
				toBegin = bufferBuilder = tesselator.getBuilder();
			} else if ((bufferBuilder = AsyncRenderer.beginBufferBuilder(particleRenderType, textureManager)) ==
					   FakeBufferBuilder.INSTANCE) {
				enableCull = cullParticles;
				syncParticles = mixedParticleRendering ? Collections.emptyList() : queue;
				tesselator = Tesselator.getInstance();
				toBegin = bufferBuilder = tesselator.getBuilder();
			} else {
				enableCull = false;
				syncParticles = AsyncRenderer.getSync(particleRenderType);
				tesselator = null;
				toBegin = FakeBufferBuilder.INSTANCE;
			}
			// must set shader before begin
			RenderSystem.setShader(GameRenderer::getParticleShader);
			particleRenderType.begin(toBegin, textureManager);
			if (!syncParticles.isEmpty()) {
				float f2 = f + 1f;
				for (Particle particle : syncParticles) {
					if (!particle.isAlive()) {
						continue;
					}
					if (enableCull && ((ParticleAddon) particle).shouldCull() &&
						!frustum.isVisible(particle.getBoundingBox())) {
						continue;
					}
					float f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					try {
						particle.render(bufferBuilder, camera, f3);
					} catch (Throwable t) {
						throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
					}
				}
			}
			profiler.popPush("upload_particles");
			// use fake, mod compatibility
			particleRenderType.end(tesselator != null
				? tesselator : CustomTesselator.of(bufferBuilder, b -> BufferUploader.drawWithShader(b.end())));
			if (bufferBuilder.building()) {
				bufferBuilder.end().release(); // release buffer manually if not released by particleRenderType.end()
			}
			profiler.pop();
		}

		profiler.push("cleanup");
		poseStack2.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		// reset blend func and culling state
		// other mods may change them...
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		lightTexture.turnOffLightLayer();
		profiler.pop();
	}
}
