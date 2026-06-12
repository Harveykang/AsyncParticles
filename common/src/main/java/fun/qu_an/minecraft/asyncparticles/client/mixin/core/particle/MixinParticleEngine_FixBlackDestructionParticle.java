package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine_FixBlackDestructionParticle {
	@Shadow
	protected ClientLevel level;

	@Dynamic
	@Inject(method = {"method_34020", "lambda$destroy$14"}, // Fabric/NeoForge
		at = @At("HEAD"))
	private void fixBlackDestroyParticle1(BlockPos blockPos, BlockState blockState, double d, double e, double f, double g, double h, double i, CallbackInfo ci) {
		int lightColor = GameUtil.getLightColorFromNeighbor(level, blockPos);
		GameUtil.DESTRUCTION_LIGHT_CACHE.set(lightColor);
	}

	@Dynamic
	@Inject(method = {"method_34020", "lambda$destroy$14"}, // Fabric/NeoForge
		at = @At("RETURN"))
	private void fixBlackDestroyParticle2(BlockPos blockPos, BlockState blockState, double d, double e, double f, double g, double h, double i, CallbackInfo ci) {
		GameUtil.DESTRUCTION_LIGHT_CACHE.remove();
	}
}
