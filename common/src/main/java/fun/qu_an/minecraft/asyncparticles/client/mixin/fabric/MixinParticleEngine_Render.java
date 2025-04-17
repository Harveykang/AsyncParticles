package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
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
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Shadow
	@Final
	public TextureManager textureManager;
	@Shadow
	@Mutable
	@Final
	public static List<ParticleRenderType> RENDER_ORDER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(LightTexture lightTexture, Camera camera, float f) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.push("prepare");
		Frustum frustum = AsyncRenderer.frustum;
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		profiler.pop();
		try {
			for (ParticleRenderType particleRenderType : RENDER_ORDER) {
				// FABRIC skips NO_RENDER
//				if (particleRenderType == ParticleRenderType.NO_RENDER) {
//					continue;
//				}
				Queue<Particle> queue = this.particles.get(particleRenderType);
				if (queue == null || queue.isEmpty()) {
					continue;
				}
				BufferBuilder bufferBuilder = AsyncRenderer.beginBufferBuilder(particleRenderType, textureManager);
				// set shader before begin
				RenderSystem.setShader(GameRenderer::getParticleShader);
				// why ParticleRenderType#end() removed?...
				RenderSystem.enableCull();
				// begin before sync particles to be compatible with some mod
				particleRenderType.begin(FakeTesselator.getFakeInstance(), this.textureManager);
				profiler.push("render_sync");
				Collection<? extends Particle> syncParticles = bufferBuilder == FakeBufferBuilder.INSTANCE
					? queue
					: AsyncRenderer.getSync(particleRenderType);
				if (!syncParticles.isEmpty()) {
					for (Particle particle : syncParticles) {
						if (!particle.isAlive()) {
							continue;
						}
						float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
						if (!frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(g))) {
							continue;
						}
						try {
							particle.render(bufferBuilder, camera, g);
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
		} finally {
			profiler.push("cleanup");
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
			profiler.pop();
		}
	}
}
