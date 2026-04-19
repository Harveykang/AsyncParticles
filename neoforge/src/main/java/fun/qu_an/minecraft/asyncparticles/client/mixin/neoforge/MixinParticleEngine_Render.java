package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
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
			if (!AsyncRenderBehavior.INSTANCE.getBTesselator(type, textureManager).shouldSync) {
				particles.put(type, GameUtil.newParticleQueue());
			}
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderBehavior.INSTANCE.getBTesselator(type, textureManager).shouldSync) {
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
	@Overwrite(remap = false)
	public void render(LightTexture lightTexture, Camera camera, float f, @Nullable Frustum ignored, Predicate<ParticleRenderType> renderTypePredicate) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		boolean renderAsync = AsyncRenderBehavior.INSTANCE.isRenderAsync();
		if (renderAsync) {
			profiler.push("wait_for_async_tasks");
			AsyncRenderBehavior.INSTANCE.tryWaitingForAsyncTasks();
			profiler.pop();
		}

		profiler.push("prepare");
		lightTexture.turnOnLightLayer();
		RenderSystem.activeTexture(33986);
		RenderSystem.activeTexture(33984);
		profiler.pop();

		Frustum frustum = AsyncRenderBehavior.INSTANCE.getFrustum();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (ParticleRenderType particleRenderType : particles.keySet()) {
			if (particleRenderType == ParticleRenderType.NO_RENDER
				|| !renderTypePredicate.test(particleRenderType)) {
				continue;
			}
			Queue<Particle> queue = this.particles.get(particleRenderType);
			if (queue == null || queue.isEmpty()) {
				continue;
			}
			BindingTesselator tesselator = AsyncRenderBehavior.INSTANCE.getBTesselator(particleRenderType, textureManager);
			profiler.push("render_sync");
			Collection<? extends Particle> syncParticles;
			ParticleCullingMode realCullMode;
			Tesselator toBegin;
			if (!renderAsync || tesselator.shouldSync) {
				realCullMode = particleCullingMode;
				syncParticles = queue;
				toBegin = Tesselator.getInstance();
			} else {
				realCullMode = ParticleCullingMode.DISABLED;
				syncParticles = AsyncRenderBehavior.INSTANCE.getSync(particleRenderType);
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
			if (bufferBuilder == null) {
				continue;
			}
			if (!syncParticles.isEmpty()) {
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
						throw AsyncRenderBehavior.INSTANCE.constructCrashReport(particle, particleRenderType, t);
					}
				}
			}
			profiler.popPush("build_buffer");
			MeshData meshData = bufferBuilder.build();
			if (meshData != null) {
				profiler.popPush("upload_particles");
				BufferUploader.drawWithShader(meshData);
			}
			profiler.pop();
		}

		profiler.push("cleanup");
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		// reset blend func and culling state
		// other mods may change them...
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		lightTexture.turnOffLightLayer();
		profiler.pop();
	}
}
