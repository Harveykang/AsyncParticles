package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render implements ParticleEngineAddon {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;
	@SuppressWarnings({"unused", "AddedMixinMembersNamePattern", "MissingUnique"})
	private static Enum<?> phase; // Iris ParticleRenderingPhase

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
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3;
			switch (particleCullingMode) {
				case AABB -> {
					f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					if (((ParticleAddon) particle).asyncparticles$shouldCull() &&
						!FrustumUtil.isVisible(frustum, ((ParticleAddon) particle).getRenderBoundingBox(f3))) {
						continue;
					}
				}
				case SPHERE -> {
					if (((ParticleAddon) particle).asyncparticles$shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
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
	@Overwrite
	// let it be public to avoid mixin conflict
	// other mods may call this method directly
	public static void renderCustomParticles(Camera camera,
											 float f,
											 MultiBufferSource.BufferSource bufferSource,
											 Queue<Particle> particles) {
		PoseStack poseStack = new PoseStack();
		Frustum frustum = AsyncRenderer.frustum;
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
					if (((ParticleAddon) particle).asyncparticles$shouldCull() &&
						!FrustumUtil.isVisible(frustum, ((ParticleAddon) particle).getRenderBoundingBox(f3))) {
						continue;
					}
				}
				case SPHERE -> {
					if (((ParticleAddon) particle).asyncparticles$shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
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
	@Overwrite
	public void render(Camera camera, float partialTick, MultiBufferSource.BufferSource bufferSource) {
		AsyncRenderer.particlePhase = true;
		List<ParticleRenderType> renderOrder = RENDER_ORDER;
		if (InternalRenderingMode.isAsync()) {
			AsyncRenderer.endAll(camera, partialTick, renderOrder);
		} else {
			for (ParticleRenderType particleRenderType : renderOrder) {
				Queue<Particle> queue = this.particles.get(particleRenderType);
				if (queue != null && !queue.isEmpty()) {
					renderParticleType(camera, partialTick, bufferSource, particleRenderType, queue);
				}
			}
		}

		if (!ModListHelper.IRIS_LIKE_LOADED || !AsyncRenderer.isTranslucentPhase(phase)) {
			Queue<Particle> queue2 = this.particles.get(ParticleRenderType.CUSTOM);
			if (queue2 != null && !queue2.isEmpty()) {
				renderCustomParticles(camera, partialTick, bufferSource, queue2);
			}
		}

		bufferSource.endBatch();
		AsyncRenderer.particlePhase = false;
	}

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (!RENDER_ORDER.contains(particleRenderType)) {
			if (!(RENDER_ORDER instanceof ArrayList<ParticleRenderType>)) {
				RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
			}
			RENDER_ORDER.add(particleRenderType);
			asyncparticle$sortRenderOrder();
		}
	}

	/**
	 * Make custom types render after non-customs.
	 * Remove duplicated render types. (e.g. Hex Casting mod's bug)
	 */
	@Override
	public void asyncparticle$sortRenderOrder() {
		List<ParticleRenderType> original = RENDER_ORDER;
		List<ParticleRenderType> renderOrder = new ArrayList<>(new LinkedHashSet<>(original));
		List<RenderType> indexHolder = new ArrayList<>(renderOrder.size());
		for (ParticleRenderType particleRenderType : renderOrder) {
			indexHolder.add(Objects.requireNonNull(particleRenderType.renderType()));
		}
		renderOrder.sort((a, b) -> {
//			if (a == b) { // Impossible
//				return 0;
//			}
			RenderType renderTypeA = a.renderType();
			RenderType renderTypeB = b.renderType();

			int iA = -1;
			int iB = -1;
			List<?> l;
			Object left, right;
			if (renderTypeA == renderTypeB) {
				// If both render types are the same, sort by particle type index
				l = renderOrder;
				left = a;
				right = b;
			} else {
				// Otherwise, sort by render type index
				l = indexHolder;
				left = renderTypeA;
				right = renderTypeB;
			}
			for (int i = 0; i < l.size(); i++) {
				Object geti = l.get(i);
				if (iA == -1 && geti == left) {
					iA = i;
				} else if (iB == -1 && geti == right) {
					iB = i;
				}
				if (iA >= 0 && iB >= 0) {
					break;
				}
			}
			return Integer.compare(iA, iB);
		});

		RENDER_ORDER = renderOrder;
	}
}
