package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.TriState;

import static net.minecraft.client.renderer.RenderStateShard.*;

// TODO: 这样行吗？
public class PhysicsModParticleRenderType {
	public static final ParticleRenderType NO_CULL_TRANSLUCENT =
		new ParticleRenderType("asyncparticles:PHYSICS_MOD_NO_CULL_TRANSLUCENT",
			RenderType.create(
				"translucent_no_cull_particle",
				DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS, 1536, false, false,
				RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_PARTICLES, TriState.FALSE, false))
					.setOutputState(PARTICLES_TARGET)
					.setLightmapState(LIGHTMAP)
					.setCullState(NO_CULL)
					.createCompositeState(false)
			));
}
