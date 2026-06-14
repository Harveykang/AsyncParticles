package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPickupParticleGroup.class)
public class MixinItemPickupParticleGroup$ParticleInstance {
	@Inject(method = "lambda$extractRenderState$0", at = @At("HEAD"))
	private static void modifyParticleRecord(Camera camera,
	                                         float partialTickTime,
	                                         ItemPickupParticle particle,
	                                         CallbackInfoReturnable<?> cir,
											 @Local(argsOnly = true, name = "partialTickTime") LocalFloatRef tickDelta) {

		if (!((ParticleAddon) particle).asyncparticles$isTicked() && partialTickTime <= 1.0f) {
			float v = partialTickTime + 1.0f;
			tickDelta.set(v);
		}
	}
}
