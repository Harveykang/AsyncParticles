package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionRainBlocking;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 5010)
public class MixinLevelRenderer_Rain {
	@Shadow
	private @Nullable ClientLevel level;

	@Inject(method = "tickRain",
		slice = @Slice(
			from = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/minecraft/client/ParticleStatus;DECREASED:Lnet/minecraft/client/ParticleStatus;")
		)
		, at = @At(value = "CONSTANT", ordinal = 0, args = "intValue=0"))
	private void onTickRain(Camera camera,
	                        CallbackInfo ci,
	                        @Local(ordinal = 0) BlockPos blockpos) {
		ContraptionRainBlocking.tickRainBlocking(
			level,
			blockpos.getX(),
			blockpos.getZ(),
			ConfigHelper.getTickRainBlockingRange());
	}

	@Inject(method = "tickRain", at = @At(value = "HEAD"))
	private void onTickRain(Camera camera,
	                        CallbackInfo ci,
	                        @Share("rainPos") LocalRef<BlockPos.MutableBlockPos> rainPosRef) {
		rainPosRef.set(new BlockPos.MutableBlockPos());
	}

	@ModifyExpressionValue(method = "tickRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/LevelReader;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
	private BlockPos wrapHeightmapPos(BlockPos blockPos,
	                                  @Share("rainPos") LocalRef<BlockPos.MutableBlockPos> rainPosRef,
	                                  @Share(value = "contraptionSurfaceY", namespace = AsyncParticlesClient.MOD_ID) LocalIntRef contraptionSurfaceY) {
		int i = Mth.ceil(ContraptionRainBlocking.getHeight(level, blockPos));
		boolean b = blockPos.getY() > i;
		if (b) {
			return blockPos;
		}
		contraptionSurfaceY.set(i); // must set after all other mods' injections.
		// record the contraption rain height,
		// we must compare it with the finally calculated rain height by other mods
		BlockPos.MutableBlockPos rainPos = rainPosRef.get();
		rainPos.set(blockPos.getX(), i, blockPos.getZ());
		return rainPos;
	}

	@WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	private int wrapGetHeight(Level instance, Heightmap.Types types, int i, int j, Operation<Integer> original) {
		return Math.max(original.call(instance, types, i, j), Mth.floor(ContraptionRainBlocking.getHeight(level, i, j)));
		// compare this with the finally calculated rain height by other mods
	}

	@WrapWithCondition(method = "tickRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private boolean wrapAddParticle(ClientLevel instance,
	                                ParticleOptions particleOptions,
	                                double d,
	                                double e,
	                                double f,
	                                double g,
	                                double h,
	                                double i,
	                                @Local(ordinal = 1) BlockPos blockpos1,
	                                @Share(value = "hasNoContraption", namespace = AsyncParticlesClient.MOD_ID) LocalBooleanRef hasNoContraption) {
		// If the block is not a contraption block, or the contraption block is not moving,
		// or the contraption y less than rain y, then add the particle
		return hasNoContraption.get()
			|| switch (ConfigHelper.getCreateRainEffect()) {
			case STATIONARY -> !ContraptionRainBlocking.isMoving(level, blockpos1);
			case NONE -> false;
			default -> true;
		};
	}
}
