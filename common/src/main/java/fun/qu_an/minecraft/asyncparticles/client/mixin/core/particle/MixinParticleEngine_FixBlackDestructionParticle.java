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

@Mixin(value = ParticleEngine.class, priority = 99)
public class MixinParticleEngine_FixBlackDestructionParticle {
	@Shadow
	protected ClientLevel level;

	@Inject(method = "destroy",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
	private void fixBlackDestroyParticle1(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
		int lightColor = GameUtil.getLightColorFromNeighbor(level, blockPos);
		GameUtil.DESTRUCTION_LIGHT_CACHE.set(lightColor);
	}
}
