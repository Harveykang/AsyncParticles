package fun.qu_an.minecraft.asyncparticles.client.compat.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;

public class IrisWeatherParticleRenderType implements ParticleRenderType {
	public static final ParticleRenderType INSTANCE = new IrisWeatherParticleRenderType();

	static {
		if (!ModListHelper.IS_FORGE) {
			((ParticleEngineAddon) Minecraft.getInstance().particleEngine).asyncparticle$addRenderType(INSTANCE);
		}
	}

	@Override
	public void begin(@NotNull BufferBuilder builder, @NotNull TextureManager textureManager) {
		if (!InternalRenderingMode.isShaderEnabled()) {
			RenderSystem.depthMask(Minecraft.useShaderTransparency());
		} else {
			Iris.getPipelineManager().getPipeline().ifPresentOrElse(
				p -> {
					if (p.getPhase() == WorldRenderingPhase.PARTICLES){
						p.setPhase(WorldRenderingPhase.RAIN_SNOW);
					}
					RenderSystem.depthMask(p.shouldWriteRainAndSnowToDepthBuffer() ||
										   Minecraft.useShaderTransparency());
				},
				() -> RenderSystem.depthMask(Minecraft.useShaderTransparency()));
		}
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getParticleShader);
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
	}

	@Override
	public void end(@NotNull Tesselator tesselator) {
		if (InternalRenderingMode.isShaderEnabled()) {
			Iris.getPipelineManager().getPipeline().ifPresent(p -> {
				if (p.getPhase() == WorldRenderingPhase.RAIN_SNOW){
					p.setPhase(WorldRenderingPhase.PARTICLES);
				}
			});
		}
		RenderSystem.depthMask(true);
		tesselator.end();
	}

	public String toString() {
		return "IRIS_WEATHER_PARTICLE_RENDER_TYPE";
	}
}
