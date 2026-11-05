package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.sable_create;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {AirFlowParticle.class, AirParticle.class, SteamJetParticle.class}, priority = 1500)
// Later than mixin.create.MixinParticle_LightCache
public abstract class MixinParticle_LightCache extends fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable.MixinParticle_LightCache {
	@Dynamic
	@TargetHandler(mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create.MixinParticle_LightCache",
		name = "asyncparticles$refresh")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
	private int wrapGetLightColor(BlockAndTintGetter level, BlockPos blockPos, Operation<Integer> original) {
		return asyncparticles$calcLight(level, blockPos, original);
	}
}
