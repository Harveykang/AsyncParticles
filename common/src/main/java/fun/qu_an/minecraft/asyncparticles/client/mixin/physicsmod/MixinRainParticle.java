package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModParticleRenderType;
import net.diebuddies.minecraft.weather.RainParticle;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RainParticle.class)
public class MixinRainParticle {
	@Redirect(method = "render", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_disableCull()V"))
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
