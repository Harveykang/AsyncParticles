package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.core.particle.render;

import com.google.common.collect.ImmutableList;
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
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render implements ParticleEngineAddon {
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
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	public void render(LightTexture lightTexture, Camera camera, float f, @Nullable Frustum ignored, Predicate<ParticleRenderType> renderTypePredicate) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();

		profiler.push("prepare");
		lightTexture.turnOnLightLayer();
		RenderSystem.activeTexture(33986);
		RenderSystem.activeTexture(33984);
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

		Frustum frustum = asyncparticle$getFrustum();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = GpuParticleBehavior.getInstance().gpuParticles;
		for (ParticleRenderType particleRenderType : CombinedIterable.ofIdentitySet(gpuParticles.keySet(), particles.keySet())) {
			if (particleRenderType == ParticleRenderType.NO_RENDER
				|| !renderTypePredicate.test(particleRenderType)) {
				continue;
			}
			Queue<Particle> queue = particles.get(particleRenderType);
			Queue<TextureSheetParticle> gpuQueue = gpuParticles.get(particleRenderType);
			boolean hasGpu = computeResult != null && gpuQueue != null && !gpuQueue.isEmpty();
			boolean hasCpu = queue != null && !queue.isEmpty();
			if (!hasCpu && !hasGpu) {
				continue;
			}
			profiler.push("render_particles");
			Collection<? extends Particle> syncParticles;
			ParticleCullingMode realCullMode;
			Tesselator toBegin;
			if (!hasCpu) {
				syncParticles = null;
				toBegin = FakeTesselator.INSTANCE;
				realCullMode = null;
			} else { // With this check we behave like vanilla if this method is called from other mod.
				realCullMode = particleCullingMode;
				syncParticles = queue;
				toBegin = Tesselator.getInstance();
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
						default -> f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
					}
					particle.render(bufferBuilder, camera, f3);
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
