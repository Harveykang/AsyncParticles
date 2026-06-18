package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemPickupParticleGroup.class)
public class MixinItemPickupParticleGroup$ParticleInstance {
	@ModifyArg(method = "lambda$extractRenderState$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ItemPickupParticleGroup$ParticleInstance;fromParticle(Lnet/minecraft/client/particle/ItemPickupParticle;Lnet/minecraft/client/Camera;F)Lnet/minecraft/client/particle/ItemPickupParticleGroup$ParticleInstance;"))
	private static float modifyParticleRecord(float deltaPartialTick,
	                                          @Local(argsOnly = true, ordinal = 0)
											  ItemPickupParticle particle) {
		return !((ParticleAddon) particle).asyncparticles$isTicked() && deltaPartialTick <= 1.0f ? deltaPartialTick + 1.0f : deltaPartialTick;
	}
}
