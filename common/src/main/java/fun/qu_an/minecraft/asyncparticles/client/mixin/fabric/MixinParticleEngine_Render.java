package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.*;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;
	@SuppressWarnings({"unused", "AddedMixinMembersNamePattern", "MissingUnique"})
	private Enum<?> phase; // Iris ParticleRenderingPhase

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private static void renderParticleType(Camera camera,
										   float f,
										   MultiBufferSource.BufferSource bufferSource,
										   ParticleRenderType particleRenderType,
										   Queue<Particle> particles) {
		VertexConsumer vertexconsumer = bufferSource.getBuffer(Objects.requireNonNull(particleRenderType.renderType()));
		Frustum frustum = AsyncRenderer.frustum;
		float f2 = f + 1f;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3 = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f2;
			if (!frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(f3))) {
				continue;
			}
			if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
				AsyncRenderer.recordSync(particleRenderType, particle);
				continue;
			}
			try {
				particle.render(vertexconsumer, camera, f3);
			} catch (Throwable var12) {
				CrashReport crashreport = CrashReport.forThrowable(var12, "Rendering Particle");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
				crashreportcategory.setDetail("Particle", particle::toString);
				crashreportcategory.setDetail("Particle Type", particleRenderType::toString);
				throw new ReportedException(crashreport);
			}
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private static void renderCustomParticles(Camera camera,
											  float f,
											  MultiBufferSource.BufferSource bufferSource,
											  Queue<Particle> particles) {
		PoseStack poseStack = new PoseStack();
		Frustum frustum = AsyncRenderer.frustum;
		float f2 = f + 1f;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3 = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f2;
			if (!frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(f3))) {
				continue;
			}
			try {
				particle.renderCustom(poseStack, bufferSource, camera, f3);
			} catch (Throwable var10) {
				CrashReport crashReport = CrashReport.forThrowable(var10, "Rendering Particle");
				CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
				crashReportCategory.setDetail("Particle", particle::toString);
				crashReportCategory.setDetail("Particle Type", "Custom");
				throw new ReportedException(crashReport);
			}
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(Camera camera, float partialTick, MultiBufferSource.BufferSource bufferSource) {
		List<ParticleRenderType> renderOrder = RENDER_ORDER;
		if (SimplePropertiesConfig.isRenderAsync()) {
			AsyncRenderer.endAll(renderOrder);
		} else {
			for (ParticleRenderType particleRenderType : renderOrder) {
				Queue<Particle> queue = this.particles.get(particleRenderType);
				if (queue != null && !queue.isEmpty()) {
					renderParticleType(camera, partialTick, bufferSource, particleRenderType, queue);
				}
			}
		}
//		for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particles.entrySet()) {
//			ParticleRenderType particleRenderType = entry.getKey();
//			if (particleRenderType != ParticleRenderType.NO_RENDER &&
//				particleRenderType.renderType() == null) {
//				renderCustomParticles(camera, partialTick, bufferSource, entry.getValue());
//			}
//		}

		if (!ModListHelper.IRIS_LIKE_LOADED || !AsyncRenderer.isTranslucentPhase(phase)) {
			Queue<Particle> queue2 = this.particles.get(ParticleRenderType.CUSTOM);
			if (queue2 != null && !queue2.isEmpty()) {
				renderCustomParticles(camera, partialTick, bufferSource, queue2);
			}
		}

		bufferSource.endBatch();
	}
}
