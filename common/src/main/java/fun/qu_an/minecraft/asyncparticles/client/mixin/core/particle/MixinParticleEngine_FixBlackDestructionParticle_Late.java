package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.particle.ParticleHelper;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, priority = 9999)
public class MixinParticleEngine_FixBlackDestructionParticle_Late {
	@Inject(method = "destroy", order = 9999,
		at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllBoxes(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
	private void fixBlackDestroyParticle2(CallbackInfo ci) {
		ParticleHelper.DESTRUCTION_LIGHT_CACHE.remove();
	}
}
