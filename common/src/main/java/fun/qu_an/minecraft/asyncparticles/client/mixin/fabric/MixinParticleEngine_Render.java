package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.*;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	protected ClientLevel level;
	@Shadow
	@Final
	public TextureManager textureManager;
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(LightTexture lightTexture, Camera camera, float f) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		boolean renderAsync = AsyncRenderer.isRenderAsync();
		if (renderAsync) {
			profiler.push("wait_for_async_tasks");
			AsyncRenderer.tryWaitForAsyncTasks();
			profiler.pop();
		}

		profiler.push("prepare");
		lightTexture.turnOnLightLayer();
		profiler.pop();

		Frustum frustum = AsyncRenderer.frustum;
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
			BindingTesselator tesselator = AsyncRenderer.getBTesselator(particleRenderType, textureManager);
			profiler.push("render_sync");
			Collection<? extends Particle> syncParticles;
			boolean enableCull;
			Tesselator toBegin;
			if (!renderAsync || tesselator.shouldSync) {
				enableCull = cullParticles;
				syncParticles = queue;
				toBegin = Tesselator.getInstance();
			} else {
				enableCull = false;
				syncParticles = AsyncRenderer.getSync(particleRenderType);
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
					float f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					if (enableCull && !frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(f3))) {
						continue;
					}
					try {
						particle.render(bufferBuilder, camera, f3);
					} catch (Throwable t) {
						throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
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
