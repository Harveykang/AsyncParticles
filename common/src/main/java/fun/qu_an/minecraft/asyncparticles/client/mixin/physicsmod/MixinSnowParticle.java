package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModParticleRenderType;
import net.diebuddies.minecraft.weather.SnowParticle;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SnowParticle.class)
public class MixinSnowParticle {
	@Redirect(method = "render", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableCull()V"))
	private void redirectDisableCull() {
		// do nothing
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public ParticleRenderType getRenderType() {
		return PhysicsModParticleRenderType.NO_CULL_TRANSLUCENT;
	}
}
