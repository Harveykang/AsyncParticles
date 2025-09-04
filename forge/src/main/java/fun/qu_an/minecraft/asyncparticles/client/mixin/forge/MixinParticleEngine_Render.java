package fun.qu_an.minecraft.asyncparticles.client.mixin.forge;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.CustomTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public class MixinParticleEngine_Render implements ParticleEngineAddon {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	@Final
	public TextureManager textureManager;
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
	}

	@Override
	public void asyncparticle$sortRenderOrder() {
		// make custom types render after non-customs
		// Remove duplicated render types, (e.g. Hex Casting mod's bug)
		Map<ParticleRenderType, Queue<Particle>> particles = new LinkedHashMap<>((int) (RENDER_ORDER.size() * 1.34) + 1);
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) != AsyncRenderer.EMPTY_FORMAT) {
				particles.put(type, GameUtil.newParticleQueue());
			}
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) == AsyncRenderer.EMPTY_FORMAT) {
				particles.put(type, GameUtil.newParticleQueue());
			}
		}

		RENDER_ORDER = ImmutableList.copyOf(particles.keySet());
		particles.putAll(this.particles);
		this.particles = particles;
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false) // Forge override
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f, Frustum ignored) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		boolean renderAsync = AsyncRenderer.isRenderAsync();
		if (renderAsync) {
			profiler.push("wait_for_async_tasks");
			AsyncRenderer.tryWaitingForAsyncTasks();
			profiler.pop();
		}

		profiler.push("prepare");
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		RenderSystem.activeTexture(33986);
		RenderSystem.activeTexture(33984);
		PoseStack poseStack2 = RenderSystem.getModelViewStack();
		poseStack2.pushPose();
		poseStack2.mulPoseMatrix(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		profiler.pop();

		Frustum frustum = AsyncRenderer.frustum;
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		boolean irisEarlyOpaquePhase = AsyncRenderer.isIrisEarlyOpaquePhase();
		// We don't use entrySet() to be compatible with iris.
		for (ParticleRenderType particleRenderType : particles.keySet()) {
			// FORGE doesn't skip NO_RENDER
			if (particleRenderType == ParticleRenderType.NO_RENDER) {
				continue;
			}
			Queue<Particle> queue = this.particles.get(particleRenderType);
			if (queue == null || queue.isEmpty()) {
				continue;
			}
			BufferBuilder bufferBuilder;
			profiler.push("render_sync");
			Collection<? extends Particle> syncParticles;
			Tesselator tesselator;
			ParticleCullingMode realCullMode;
			BufferBuilder toBegin;
			if ((bufferBuilder = AsyncRenderer.beginBufferBuilder(particleRenderType, textureManager)) ==
				FakeBufferBuilder.INSTANCE) {
				realCullMode = particleCullingMode;
				// if irisEarlyOpaquePhase, we render custom particles in AsyncRenderer.irisCustom()
				syncParticles = irisEarlyOpaquePhase ? Collections.emptyList() : queue;
				tesselator = Tesselator.getInstance();
				toBegin = bufferBuilder = tesselator.getBuilder();
			} else if (!renderAsync) {
				realCullMode = particleCullingMode;
				syncParticles = queue;
				tesselator = Tesselator.getInstance();
				toBegin = bufferBuilder = tesselator.getBuilder();
			} else {
				realCullMode = ParticleCullingMode.DISABLED;
				syncParticles = AsyncRenderer.getSync(particleRenderType);
				tesselator = CustomTesselator.of(bufferBuilder, b -> BufferUploader.drawWithShader(b.end()));
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
					ParticleAddon particleAddon = (ParticleAddon) particle;
					switch (realCullMode) {
						case AABB -> {
							if (particleAddon.shouldCull() &&
								!FrustumUtil.isVisible(frustum, particle.getBoundingBox())) {
								continue;
							}
						}
						case SPHERE -> {
							if (particleAddon.shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
								continue;
							}
						}
						case ASYNC_AABB, ASYNC_SPHERE -> {
							if (particleAddon.shouldCull() &&
								!particleAddon.asyncparticles$isVisibleOnScreen()) {
								continue;
							}
						}
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
			particleRenderType.end(tesselator);
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
