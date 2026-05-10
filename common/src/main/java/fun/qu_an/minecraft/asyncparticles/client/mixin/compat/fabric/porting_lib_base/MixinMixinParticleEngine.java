package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.fabric.porting_lib_base;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 550)
public abstract class MixinMixinParticleEngine implements ParticleEngineAddon {
	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick.MixinParticleEngine",
		name = "asyncparticles$newParticleQueue"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("RETURN"))
	private void addCustomRenderTypes(ParticleRenderType k,
									  CallbackInfoReturnable<Queue<Particle>> cir) {
		asyncparticle$addRenderType(k);
	}
}
