package fun.qu_an.minecraft.asyncparticles.client.mixin.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.CustomTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	private TextureManager textureManager;

	@Shadow
	@Final
	private static Logger LOGGER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false) // Forge override
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f, @Nullable Frustum ignored) {
		PoseStack poseStack2 = null;
		Frustum frustum = AsyncRenderer.frustum;
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		if (!AsyncRenderer.isStart) {
			lightTexture.turnOnLightLayer();
			RenderSystem.enableDepthTest();
			poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushPose();
			poseStack2.mulPoseMatrix(poseStack.last().pose());
			RenderSystem.applyModelViewMatrix();
		}

		try {
			// We don't use entrySet() to compatible with iris.
			for (ParticleRenderType particleRenderType : particles.keySet()) {
				// FORGE doesn't skip NO_RENDER
				if (particleRenderType == ParticleRenderType.NO_RENDER) {
					continue;
				}
				Queue<Particle> iterable = this.particles.get(particleRenderType);
				if (iterable == null || iterable.isEmpty()) {
					continue;
				}
				BufferBuilder bufferBuilder = AsyncRenderer.beginBufferBuilder(particleRenderType, textureManager);
				if (!AsyncRenderer.isStart) {
					profiler.push("sync_particles");
					Collection<? extends Particle> particles1 = bufferBuilder == FakeBufferBuilder.INSTANCE
						? iterable
						: AsyncRenderer.getSync(particleRenderType);
					// begin before sync particles to be compatible with some mod
					RenderSystem.setShader(GameRenderer::getParticleShader);
					particleRenderType.begin(FakeBufferBuilder.INSTANCE, this.textureManager);
					if (!particles1.isEmpty()) {
						for (Particle particle : particles1) {
							if (!particle.isAlive()) {
								continue;
							}
							if (particle.shouldCull() && !frustum.isVisible(particle.getBoundingBox())) {
								continue;
							}
							float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
							try {
								particle.render(bufferBuilder, camera, g);
							} catch (Throwable throwable) {
								CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Particle");
								CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
								Objects.requireNonNull(particle);
								crashReportCategory.setDetail("Particle", particle::toString);
								Objects.requireNonNull(particleRenderType);
								crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
								throw new ReportedException(crashReport);
							}
						}
					}
					profiler.pop();
					if (!bufferBuilder.building()) {
						particleRenderType.end(FakeTesselator.INSTANCE);
						// call end to be compatible with some mod
						continue;
					}
					profiler.push("upload_particles");
					// use fake, mod compatibility
					particleRenderType.end(bufferBuilder == FakeBufferBuilder.INSTANCE
						? FakeTesselator.INSTANCE
						: CustomTesselator.of(bufferBuilder, b -> BufferUploader.drawWithShader(b.end())));
					if (bufferBuilder.building()) {
						bufferBuilder.end().release(); // release buffer manually if not released by particleRenderType.end()
					}
					profiler.pop();
				} else {
					if (bufferBuilder == FakeBufferBuilder.INSTANCE) {
						continue;
					}
					profiler.push("async_particles");
					Runnable runnable = () -> iterable.forEach(particle -> {
						if (!particle.isAlive()) {
							return;
						}
						if (particle.shouldCull() && !frustum.isVisible(particle.getBoundingBox())) {
							return;
						}
						if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
							AsyncRenderer.recordSync(particleRenderType, particle);
							return;
						}
						float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
						try {
							particle.render(bufferBuilder, camera, g);
						} catch (Throwable throwable) {
							LOGGER.error("Exception while rendering particle {}, marking as sync", particle, throwable);
							((ParticleAddon) particle).asyncedParticles$setRenderSync();
							AsyncRenderer.markAsSync(particle.getClass());
							AsyncRenderer.recordSync(particleRenderType, particle);
						}
					});
					AsyncRenderer.add(runnable);
					profiler.pop();
				}
			}
		} finally {
			// make sure poseStack2 is popped
			if (!AsyncRenderer.isStart) {
				poseStack2.popPose();
				RenderSystem.applyModelViewMatrix();
				RenderSystem.depthMask(true);
				RenderSystem.disableBlend();
				lightTexture.turnOffLightLayer();
			}
		}
	}
}
