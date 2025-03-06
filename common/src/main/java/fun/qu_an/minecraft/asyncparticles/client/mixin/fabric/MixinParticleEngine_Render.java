package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.util.CustomableEndTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBeginBufferBuilder;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Shadow
	@Final
	private TextureManager textureManager;

	@Shadow
	@Final
	private static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Final
	private static Logger LOGGER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
		PoseStack poseStack2 = null;
		Frustum frustum = AsyncRenderer.frustum;
		if (!AsyncRenderer.isStart) {
			lightTexture.turnOnLightLayer();
			RenderSystem.enableDepthTest();
			poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushPose();
			poseStack2.mulPoseMatrix(poseStack.last().pose());
			RenderSystem.applyModelViewMatrix();
		}
//		System.out.println(RENDER_ORDER);
//		System.out.println(particles.keySet());
		// Some mod has duplicated render type, cause concurrent access to the same queue
		// See MixinParticleEngine_Late.java
//		assert !ModListHelper.IS_FORGE;
		for (ParticleRenderType particleRenderType : RENDER_ORDER) {
			// FABRIC skips NO_RENDER
//			if (particleRenderType == ParticleRenderType.NO_RENDER) {
//				continue;
//			}
			Queue<Particle> iterable = this.particles.get(particleRenderType);
			if (iterable == null || iterable.isEmpty()) {
				continue;
			}
			BufferBuilder bufferBuilder = AsyncRenderer.beginBufferBuilder(particleRenderType, textureManager);
			if (!AsyncRenderer.isStart) {
				Collection<? extends Particle> particles1 = bufferBuilder == FakeBeginBufferBuilder.INSTANCE
					? iterable
					: AsyncRenderer.getSync(particleRenderType);
				if (!particles1.isEmpty()) {
					for (Particle particle : particles1) {
//						if (!frustum.isVisible(particle.getBoundingBox())) {
//							continue;
//						}
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
				RenderSystem.setShader(GameRenderer::getParticleShader);
				// use fake, mod compatibility
				particleRenderType.begin(FakeBeginBufferBuilder.INSTANCE, this.textureManager);
				particleRenderType.end(CustomableEndTesselator.INSTANCE.onEnd(() -> BufferUploader.drawWithShader(bufferBuilder.end())));
				if (bufferBuilder.building()) {
					bufferBuilder.end().release(); // release buffer manually if not released by particleRenderType.end()
				}
			} else {
				if (bufferBuilder == FakeBeginBufferBuilder.INSTANCE) {
					continue;
				}
				Runnable runnable = () -> iterable.forEach(particle -> {
					if (((ParticleAddon) particle).shouldCull() && !frustum.isVisible(particle.getBoundingBox())) {
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
			}
		}

		if (!AsyncRenderer.isStart) {
			poseStack2.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
		}
	}
}
