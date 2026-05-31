package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {
	ItemPickupParticleGroup.class,
	ElderGuardianParticleGroup.class
})
public class MixinAsyncTick_ModifyFromParticleMethod {
	@Dynamic
	@Inject(method = "*", at = @At("HEAD"))
	private static void modifyParticleRecord(@Coerce Particle particle,
											 Camera camera,
											 float f,
											 CallbackInfoReturnable<?> cir,
											 @Local(ordinal = 0, argsOnly = true) LocalFloatRef tickDelta) {
		float v = !((ParticleAddon) particle).asyncparticles$isTicked() && f <= 1.0f ? f + 1.0f : f;
		tickDelta.set(v);
	}
}
