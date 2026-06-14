package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ElderGuardianParticle;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElderGuardianParticleGroup.class)
public class MixinElderGuardianParticleGroup$ElderGuardianParticleRenderState {
	@Inject(method = "lambda$extractRenderState$0", at = @At("HEAD"))
	private static void modifyParticleRecord(Camera camera,
	                                         float partialTickTime,
	                                         ElderGuardianParticle particle,
	                                         CallbackInfoReturnable<?> cir,
	                                         @Local(argsOnly = true, name = "partialTickTime") LocalFloatRef tickDelta) {

		if (!((ParticleAddon) particle).asyncparticles$isTicked() && partialTickTime <= 1.0f) {
			float v = partialTickTime + 1.0f;
			tickDelta.set(v);
		}
	}
}
