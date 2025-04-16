package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.physicsmod;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 1100)
public class MixinLevelRenderer {
	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
	private boolean onAddAlwaysVisibleParticle(ClientLevel instance, ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return !VSClientUtils.isUnderShipHeightMap(instance, x, y, z);
	}
}
