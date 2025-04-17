package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

public class PhysicsModParticleRenderType {
	public static final ParticleRenderType NO_CULL_TRANSLUCENT = new ParticleRenderType() {
		public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
			RenderSystem.depthMask(true);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			RenderSystem.enableBlend();
			// The other mods may not call enableCull() in this method.
			// To avoid side effects,
			// we have to enable culling before this method,
			// see MixinParticleEngine.render
			RenderSystem.disableCull();
			RenderSystem.defaultBlendFunc();
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		public String toString() {
			return "asyncparticles:PHYSICS_MOD_NO_CULL_TRANSLUCENT";
		}
	};
}
