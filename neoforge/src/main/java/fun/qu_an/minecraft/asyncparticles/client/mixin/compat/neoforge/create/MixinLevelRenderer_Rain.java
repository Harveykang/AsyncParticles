package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.ContraptionRainBlocking;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer_Rain {
	@Shadow
	private @Nullable ClientLevel level;
	@Unique
	private static final long asyncparticles$MASK = BlockPos.asLong(-1, 0, -1);

	@Inject(method = "tickRain",
		slice = @Slice(
			from = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/minecraft/client/ParticleStatus;DECREASED:Lnet/minecraft/client/ParticleStatus;")
		)
		, at = @At(value = "CONSTANT", ordinal = 0, args = "intValue=0"))
	private void onTickRain(Camera camera,
	                        CallbackInfo ci,
	                        @Local(ordinal = 0) int range,
	                        @Local(ordinal = 0) BlockPos blockpos) {
		ContraptionRainBlocking.tickRainBlocking(
			level,
			blockpos.getX() - range,
			blockpos.getZ() - range,
			blockpos.getX() + range,
			blockpos.getZ() + range);
	}

	@Inject(method = "tickRain", at = @At(value = "HEAD"))
	private void onTickRain(Camera camera,
	                        CallbackInfo ci,
	                        @Share("rainPos") LocalRef<BlockPos.MutableBlockPos> rainPos) {
		rainPos.set(new BlockPos.MutableBlockPos());
	}

	@ModifyExpressionValue(method = "tickRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/LevelReader;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
	private BlockPos wrapHeightmapPos(BlockPos original,
	                                  @Share("hasNoContraption") LocalBooleanRef hasNoContraption,
	                                  @Share("rainPos") LocalRef<BlockPos.MutableBlockPos> rainPos) {
		int i = Mth.ceil(ContraptionRainBlocking.getAttachedContractionHeightMap(level)
			.get(original.asLong() & asyncparticles$MASK));
		boolean b = original.getY() > i;
		hasNoContraption.set(b);
		if (b) {
			return original;
		}
		BlockPos.MutableBlockPos pos = rainPos.get();
		pos.set(original.getX(), i, original.getZ());
		return pos;
	}

	@WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	private int wrapGetHeight(Level instance, Heightmap.Types types, int i, int j, Operation<Integer> original) {
		return Math.max(original.call(instance, types, i, j), Mth.ceil(ContraptionRainBlocking.getAttachedContractionHeightMap(level)
			.get(BlockPos.asLong(i, 0, j))));
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
	                                @Share("hasNoContraption") LocalBooleanRef hasNoContraption) {
		// If the block is not a contraption block, or the contraption block is not moving,
		// or the contraption y less than rain y, then add the particle
		return hasNoContraption.get()
			|| switch (ConfigHelper.getCreateRainEffect()) {
			case STATIONARY -> !ContraptionRainBlocking.getAttachedContractionMovingMap(level)
				.get(blockpos1.asLong() & asyncparticles$MASK);
			case NONE -> false;
			default -> true;
		};
	}
}
