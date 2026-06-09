package fun.qu_an.minecraft.asyncparticles.client.mixin.core.animate_tick;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.core.AnimateTickBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WaterFluid.class)
public class MixinWaterFluid {
	@WrapWithCondition(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	public boolean cullUnderWaterParticleType(Level instance, ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
		// assert level instanceof ClientLevel;
		return AnimateTickBehavior.CULL_UNDERWATER_PARTICLE_TYPE.get();
	}
}
