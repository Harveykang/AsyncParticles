package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.compat.porting_lib_base;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick.MixinParticleEngine;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 550)
public abstract class MixinMixinParticleEngine implements ParticleEngineAddon {
	/**
	 * @see MixinParticleEngine#asyncparticles$newParticleQueue(ParticleRenderType)
	 */
	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick.MixinParticleEngine",
		name = "asyncparticles$newParticleQueue"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("HEAD"))
	private void addCustomRenderTypes(ParticleRenderType particleRenderType,
									  CallbackInfoReturnable<Queue<?>> cir) {
		asyncparticle$addRenderType(particleRenderType);
	}
}
