package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@SuppressWarnings({"unused", "AddedMixinMembersNamePattern", "MissingUnique"})
	private static Enum<?> phase; // Iris ParticleRenderingPhase

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	private static void renderParticleType(Camera camera,
										   float f,
										   MultiBufferSource.BufferSource bufferSource,
										   ParticleRenderType particleRenderType,
										   Queue<Particle> particles,
										   @Nullable Frustum frustum) {
		VertexConsumer vertexconsumer = bufferSource.getBuffer(Objects.requireNonNull(particleRenderType.renderType()));
		if (frustum == null) {
			frustum = AsyncRenderer.frustum;
		}
		float f2 = f + 1f;
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3;
			switch (particleCullingMode) {
				case AABB -> {
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					if (((ParticleAddon) particle).shouldCull() &&
						!FrustumUtil.isVisible(frustum, ((ParticleAddon) particle).getRenderBoundingBox(f3))) {
						continue;
					}
				}
				case SPHERE -> {
					if (((ParticleAddon) particle).shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
						continue;
					}
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
				}
				case ASYNC_AABB, ASYNC_SPHERE -> {
					if (!((ParticleAddon) particle).asyncparticles$isVisibleOnScreen()) {
						continue;
					}
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
				}
				default -> f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
			}
			if (((ParticleAddon) particle).asyncparticles$isRenderSync()) {
				AsyncRenderer.recordSync(particleRenderType, particle);
				continue;
			}
			try {
				particle.render(vertexconsumer, camera, f3);
			} catch (Throwable t) {
				throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
			}
		}
	}

	/**
	 * @author
	 * @reason
	 */
	// let it be public to avoid mixin conflict
	// other mods may call this method directly
	@Overwrite(remap = false)
	public static void renderCustomParticles(Camera camera, float f, MultiBufferSource.
		BufferSource bufferSource, Queue<Particle> particles, Frustum frustum) {
		PoseStack poseStack = new PoseStack();
		if (frustum == null) {
			// set frustum here because other mods may call this method directly
			frustum = AsyncRenderer.frustum;
		}
		float f2 = f + 1f;
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3;
			switch (particleCullingMode) {
				case AABB -> {
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					if (((ParticleAddon) particle).shouldCull() &&
						!FrustumUtil.isVisible(frustum, ((ParticleAddon) particle).getRenderBoundingBox(f3))) {
						continue;
					}
				}
				case SPHERE -> {
					if (((ParticleAddon) particle).shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
						continue;
					}
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
				}
				case ASYNC_AABB, ASYNC_SPHERE -> {
					if (!((ParticleAddon) particle).asyncparticles$isVisibleOnScreen()) {
						continue;
					}
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
				}
				default -> f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
			}
			try {
				particle.renderCustom(poseStack, bufferSource, camera, f3);
			} catch (Throwable t) {
				throw AsyncRenderer.constructCrashReport(particle, particle.getRenderType(), t);
			}
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	public void render(Camera camera,
					   float partialTick,
					   MultiBufferSource.BufferSource bufferSource,
					   @Nullable Frustum frustum,
					   Predicate<ParticleRenderType> renderTypePredicate) {
		Set<ParticleRenderType> renderOrder = particles.keySet();
		if (InternalRenderingMode.isAsync()) {
			AsyncRenderer.endParticles(camera, partialTick, renderOrder, renderTypePredicate);
		} else {
			for (ParticleRenderType particleRenderType : renderOrder) {
				if (particleRenderType.renderType() == null) {
					continue;
				}
				if (!renderTypePredicate.test(particleRenderType)) {
					continue;
				}
				Queue<Particle> queue = this.particles.get(particleRenderType);
				if (queue != null && !queue.isEmpty()) {
					renderParticleType(camera, partialTick, bufferSource, particleRenderType, queue, frustum);
				}
			}
		}

		if (renderTypePredicate.test(ParticleRenderType.PARTICLE_SHEET_OPAQUE)) {
			Queue<Particle> queue2 = this.particles.get(ParticleRenderType.CUSTOM);
			if (queue2 != null && !queue2.isEmpty()) {
				renderCustomParticles(camera, partialTick, bufferSource, queue2, frustum);
			}
		}

		bufferSource.endBatch();
	}
}
