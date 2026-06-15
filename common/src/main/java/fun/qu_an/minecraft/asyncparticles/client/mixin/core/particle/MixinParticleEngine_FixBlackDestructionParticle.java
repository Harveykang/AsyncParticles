package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine_FixBlackDestructionParticle {
	@Shadow
	protected ClientLevel level;

	@Inject(method = "destroy", order = 99,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
	private void fixBlackDestroyParticle1(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
		int lightColor = GameUtil.getLightColorFromNeighbor(level, blockPos);
		GameUtil.DESTRUCTION_LIGHT_CACHE.set(lightColor);
	}

	@Inject(method = "destroy", order = 9999,
		at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllBoxes(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
	private void fixBlackDestroyParticle2(CallbackInfo ci) {
		GameUtil.DESTRUCTION_LIGHT_CACHE.remove();
	}
}
