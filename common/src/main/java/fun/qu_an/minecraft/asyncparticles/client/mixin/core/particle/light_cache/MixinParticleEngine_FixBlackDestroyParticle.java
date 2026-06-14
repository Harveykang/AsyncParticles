package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.core.GameUtil;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class)
public class MixinParticleEngine_FixBlackDestroyParticle {
	@Inject(method = "addDestroyBlockEffect", order = 99, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
	private void fixBlackDestroyParticle1(BlockPos pos, BlockState blockState, CallbackInfo ci) {
		int lightColor = GameUtil.getLightColorFromNeighbor((ClientLevel) (Object) this, pos);
		ParticleHelper.DESTRUCTION_LIGHT_CACHE.set(lightColor);
	}

	@Inject(method = "addDestroyBlockEffect", order = 9999, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllBoxes(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
	private void fixBlackDestroyParticle2(BlockPos pos, BlockState blockState, CallbackInfo ci) {
		ParticleHelper.DESTRUCTION_LIGHT_CACHE.remove();
	}
}
