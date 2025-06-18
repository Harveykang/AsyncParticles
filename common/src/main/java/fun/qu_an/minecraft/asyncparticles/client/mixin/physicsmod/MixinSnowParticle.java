package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModParticleRenderType;
import net.diebuddies.minecraft.weather.SnowParticle;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SnowParticle.class)
public class MixinSnowParticle {
	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public ParticleRenderType getRenderType() {
		return PhysicsModParticleRenderType.NO_CULL_TRANSLUCENT;
	}
}
