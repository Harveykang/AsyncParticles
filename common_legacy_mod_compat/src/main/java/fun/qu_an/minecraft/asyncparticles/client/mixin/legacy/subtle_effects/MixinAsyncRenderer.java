package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.subtle_effects;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.compat.subtle_effects.SubtleEffectsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AsyncRenderBehavior.class)
public class MixinAsyncRenderer {
	@Redirect(method = "renderParticles",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;isAlive()Z"))
	private static boolean shouldRenderParticle(Particle instance,
												@Local(argsOnly = true) Camera camera,
												@Local(argsOnly = true) ParticleRenderType renderType) {
		return instance.isAlive() && SubtleEffectsCompat.shouldRenderParticle(instance, camera, renderType);
	}
}
