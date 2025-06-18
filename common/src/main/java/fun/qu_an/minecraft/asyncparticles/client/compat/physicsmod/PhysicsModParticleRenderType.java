package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.TriState;

import static net.minecraft.client.renderer.RenderStateShard.*;

// TODO: 这样行吗？
public class PhysicsModParticleRenderType {
	public static final RenderPipeline NO_CULL_TRANSLUCENT_PARTICLE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
			.withCull(false)
			.withLocation("pipeline/translucent_particle")
			.withBlend(BlendFunction.TRANSLUCENT)
			.build()
	);

	public static final ParticleRenderType NO_CULL_TRANSLUCENT =
		new ParticleRenderType("asyncparticles:PHYSICS_MOD_NO_CULL_TRANSLUCENT",
			RenderType.create(
				"translucent_particle",
				1536,
				false,
				false,
				NO_CULL_TRANSLUCENT_PARTICLE,
				RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_PARTICLES, false))
					.setOutputState(PARTICLES_TARGET)
					.setLightmapState(LIGHTMAP)
					.createCompositeState(false)
			));
}
