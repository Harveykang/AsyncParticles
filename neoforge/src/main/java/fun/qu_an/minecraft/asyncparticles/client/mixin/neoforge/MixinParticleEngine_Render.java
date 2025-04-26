package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
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
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Render {
	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	public TextureManager textureManager;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	public void render(LightTexture lightTexture, Camera camera, float f, @Nullable Frustum ignored, Predicate<ParticleRenderType> renderTypePredicate) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.push("prepare");
		Frustum frustum = AsyncRenderer.frustum;
		lightTexture.turnOnLightLayer();
//		RenderSystem.enableDepthTest();
		RenderSystem.activeTexture(33986);
		RenderSystem.activeTexture(33984);
		profiler.pop();

		for (ParticleRenderType particleRenderType : particles.keySet()) {
			if (particleRenderType == ParticleRenderType.NO_RENDER
				|| !renderTypePredicate.test(particleRenderType)) {
				continue;
			}
			Queue<Particle> queue = this.particles.get(particleRenderType);
			if (queue == null || queue.isEmpty()) {
				continue;
			}
			BindingTesselator tesselator = AsyncRenderer.getBTesselator(particleRenderType, textureManager);
			// set shader before begin
			RenderSystem.setShader(GameRenderer::getParticleShader);
			// why ParticleRenderType#end() removed?...
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			// begin before sync particles to be compatible with some mod
			particleRenderType.begin(FakeTesselator.getFakeInstance(), this.textureManager);
			profiler.push("render_sync");
			Collection<? extends Particle> syncParticles = tesselator.custom
				? queue
				: AsyncRenderer.getSync(particleRenderType);
			BufferBuilder bufferBuilder;
			if (syncParticles.isEmpty()) {
				bufferBuilder = tesselator.getBuilder();
			} else {
				bufferBuilder = tesselator.begin();
				for (Particle particle : syncParticles) {
					if (!particle.isAlive()) {
						continue;
					}
					float g = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f + 1f;
					if (SimplePropertiesConfig.isCullParticles() && !frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(g))) {
						continue;
					}
					try {
						particle.render(bufferBuilder, camera, g);
					} catch (Throwable t) {
						throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
					}
				}
			}
			if (bufferBuilder.building) {
				profiler.popPush("build_buffer");
				MeshData meshData = bufferBuilder.build();
				if (meshData != null) {
					profiler.popPush("upload_particles");
					BufferUploader.drawWithShader(meshData);
				}
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
