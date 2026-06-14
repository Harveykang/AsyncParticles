package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.ElderGuardianParticle;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ElderGuardianParticleGroup.class)
public class MixinElderGuardianParticleGroup$ElderGuardianParticleRenderState {
	@ModifyArg(method = "lambda$extractRenderState$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ElderGuardianParticleGroup$ElderGuardianParticleRenderState;fromParticle(Lnet/minecraft/client/particle/ElderGuardianParticle;Lnet/minecraft/client/Camera;F)Lnet/minecraft/client/particle/ElderGuardianParticleGroup$ElderGuardianParticleRenderState;"))
	private static float modifyParticleRecord(float partialTickTime,
	                                          @Local(argsOnly = true, name = "particle")
											  ElderGuardianParticle particle) {
		return !((ParticleAddon) particle).asyncparticles$isTicked() && partialTickTime <= 1.0f ? partialTickTime + 1.0f : partialTickTime;
	}
}
