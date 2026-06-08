package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.core.GameUtil;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.fix_black_destruction_particle.TerrainParticleBehavior;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinParticleEngine_FixBlackDestroyParticle {
	@Inject(method = "addDestroyBlockEffect", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllBoxes(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
	private void fixBlackDestroyParticle1(BlockPos pos, BlockState blockState, CallbackInfo ci) {
		int lightColor = GameUtil.getLightColorFromNeighbor((ClientLevel) (Object) this, pos);
		TerrainParticleBehavior.DESTRUCTION_LIGHT_CACHE.set(lightColor);
	}

	@Inject(method = "addDestroyBlockEffect", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllBoxes(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
	private void fixBlackDestroyParticle2(BlockPos pos, BlockState blockState, CallbackInfo ci) {
		TerrainParticleBehavior.DESTRUCTION_LIGHT_CACHE.remove();
	}
}
