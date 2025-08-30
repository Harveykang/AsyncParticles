package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WaterFluid.class)
public class MixinWaterFluid {
	@WrapWithCondition(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	public boolean cullUnderWaterParticleType(Level instance, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		// assert level instanceof ClientLevel;
		return !ConfigHelper.isCullUnderwaterParticleType() ||
			   Minecraft.useShaderTransparency() ||
			   Minecraft.getInstance().player.isUnderWater();
	}
}
