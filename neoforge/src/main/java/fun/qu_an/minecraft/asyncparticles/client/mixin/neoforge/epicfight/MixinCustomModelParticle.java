package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.epicfight;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.particle.CustomModelParticle;

@Mixin(CustomModelParticle.class)
public abstract class MixinCustomModelParticle implements ParticleAddon {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		asyncparticles$setRenderSync();
		Class<? extends Particle> clazz = asyncparticles$getRealClass();
		if (!AsyncRenderBehavior.INSTANCE.shouldSync(clazz)) {
			AsyncRenderBehavior.INSTANCE.markAsSync(clazz);
		}
	}
}
