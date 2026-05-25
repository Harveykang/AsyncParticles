package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.lambdynlights;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, priority = 1500)
public class MixinParticleEngineMixin {
	@TargetHandler(
		name = "lambdynlights$onTick",
		mixin = "dev.lambdaurora.lambdynlights.mixin.ParticleEngineMixin"
	)
	@WrapMethod(method = "@MixinSquared:Handler")
	private void injectOnTick(Particle particle, CallbackInfo ci, Operation<Void> original) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> original.call(particle, ci));
		} else {
			original.call(particle, ci);
		}
	}
}
