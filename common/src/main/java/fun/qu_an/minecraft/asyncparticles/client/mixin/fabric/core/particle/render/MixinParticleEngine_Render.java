package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.core.particle.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.RenderExceptionHandler;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public class MixinParticleEngine_Render implements ParticleEngineAddon {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	@Final
	public TextureManager textureManager;
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Final
	private TextureAtlas textureAtlas;

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (!RENDER_ORDER.contains(particleRenderType)) {
			if (!(RENDER_ORDER instanceof ArrayList<ParticleRenderType>)) {
				RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
			}
			RENDER_ORDER.add(particleRenderType);
		}
	}

	@Override
	public void asyncparticle$sortRenderOrder() {
		// make custom types render after non-customs
		// Remove duplicated render types, (e.g. Hex Casting mod's bug)
		Set<ParticleRenderType> renderTypes = new LinkedHashSet<>((int) (RENDER_ORDER.size() * 1.34) + 1);
		for (ParticleRenderType type : RENDER_ORDER) {
			if (!AsyncRenderBehavior.getInstance().getBTesselator(type, textureManager).shouldSync) {
				renderTypes.add(type);
			}
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderBehavior.getInstance().getBTesselator(type, textureManager).shouldSync) {
				renderTypes.add(type);
			}
		}
		RENDER_ORDER = new ArrayList<>(renderTypes);
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(LightTexture lightTexture, Camera camera, float f) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		boolean renderAsync = AsyncRenderBehavior.getInstance().isRenderAsync();
		if (renderAsync) {
			profiler.push("wait_for_async_tasks");
			AsyncRenderBehavior.getInstance().tryWaitingForAsyncTasks();
			profiler.pop();
		}

		profiler.push("prepare");
		lightTexture.turnOnLightLayer();
		textureAtlas.setFilter(false, textureAtlas.mipmap);
		profiler.pop();

		ComputeResult computeResult = null;
		IParticleRenderer particleRenderer = null;
		if (ConfigHelper.isGpuParticles()) {
			if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.PARTICLE_RENDERING) {
				GpuParticleBehavior.getInstance().compute(camera, f);
			}
			computeResult = GpuParticleBehavior.getInstance().ensureComputeReady();
			if (computeResult != null) {
				particleRenderer = GpuParticleBehavior.getInstance().getOrCreateRenderer();
			}
		}

		Frustum frustum = AsyncRenderBehavior.getInstance().getFrustum();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (ParticleRenderType particleRenderType : RENDER_ORDER) {
			// FABRIC skips NO_RENDER
//				if (particleRenderType == ParticleRenderType.NO_RENDER) {
//					continue;
//				}
			Queue<Particle> queue = this.particles.get(particleRenderType);
			Queue<TextureSheetParticle> gpuQueue = GpuParticleBehavior.getInstance().gpuParticles.get(particleRenderType);
			boolean hasGpu = computeResult != null && gpuQueue != null && !gpuQueue.isEmpty();
			boolean hasCpu = queue != null && !queue.isEmpty();
			if (!hasCpu && !hasGpu) {
				continue;
			}
			profiler.push("render_particles");
			BindingTesselator tesselator = AsyncRenderBehavior.getInstance().getBTesselator(particleRenderType, textureManager);
			Collection<? extends Particle> syncParticles;
			ParticleCullingMode realCullMode;
			Tesselator toBegin;
			// With this check we behave like vanilla if this method is called from other mod.
			if (!hasCpu) {
				syncParticles = null;
				toBegin = FakeTesselator.INSTANCE;
				realCullMode = null;
			} else if (AsyncRenderBehavior.getInstance().getBTesselator(particleRenderType, textureManager).shouldSync || !renderAsync) {
				realCullMode = particleCullingMode;
				syncParticles = queue;
				toBegin = Tesselator.getInstance();
			} else {
				realCullMode = ParticleCullingMode.DISABLED;
				syncParticles = AsyncRenderBehavior.getInstance().getSync(particleRenderType);
				toBegin = tesselator;
			}
			// why ParticleRenderType#end() removed?...
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			// set shader before begin
			RenderSystem.setShader(GameRenderer::getParticleShader);
			// begin before sync particles to be compatible with some mod
			// We must ensure only call begin once in this method,
			// otherwise it will mess up some mod's mixins.
			BufferBuilder bufferBuilder = particleRenderType.begin(toBegin, this.textureManager);
			if (hasGpu) {
				particleRenderer.render(particleRenderType);
			}
			if (bufferBuilder != null && hasCpu && !syncParticles.isEmpty()) {
				float f2 = f + 1f;
				for (Particle particle : syncParticles) {
					if (!particle.isAlive()) {
						continue;
					}
					float f3;
					ParticleAddon particleAddon = (ParticleAddon) particle;
					switch (realCullMode) {
						case AABB -> {
							f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
							if (particleAddon.asyncparticles$shouldCull() &&
								!FrustumUtil.isVisible(frustum, particleAddon.getRenderBoundingBox(f3))) {
								continue;
							}
						}
						case SPHERE -> {
							if (particleAddon.asyncparticles$shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
								continue;
							}
							f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
						}
						case ASYNC_AABB, ASYNC_SPHERE -> {
							if (particleAddon.asyncparticles$shouldCull() &&
								!particleAddon.asyncparticles$isVisibleOnScreen()) {
								continue;
							}
							f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
						}
						default -> f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
					}
					try {
						particle.render(bufferBuilder, camera, f3);
					} catch (Throwable t) {
						if (RenderExceptionHandler.getInstance().onRenderOnMainThreadExceptionally(t, particle, particleRenderType)) {
							break;
						}
					}
				}
			}
			profiler.popPush("build_buffer");
			// Write like this to be compatible with TenshiLib
			if (bufferBuilder == null) {
				bufferBuilder = FakeBufferBuilder.INSTANCE;
			}
			MeshData meshData = bufferBuilder.build();
			if (meshData != null) {
				profiler.popPush("upload_particles");
				BufferUploader.drawWithShader(meshData);
			}
			// Qliphoth Awakening injects end() here so we can't use continue after begin() in this loop
			profiler.pop();
		}

		profiler.push("cleanup");
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
