package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedWriteMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelChunk.class, priority = 1500)
public abstract class MixinLevelChunk_SafeBlockEntityMap_Off extends ChunkAccess {
	@Shadow
	@Final
	private Level level;

	public MixinLevelChunk_SafeBlockEntityMap_Off(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory containerFactory, long inhabitedTime, LevelChunkSection @Nullable [] sections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, containerFactory, inhabitedTime, sections, blendingData);
	}

	@Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
		at = @At("HEAD"), cancellable = true)
	private void onGetBlockEntity(BlockPos pos,
	                              LevelChunk.EntityCreationType creationType,
	                              CallbackInfoReturnable<BlockEntity> cir) {
		if (level.isClientSide() && ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(blockEntities.get(pos));
		}
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit1(CallbackInfo ci) {
		if (level.isClientSide()) {
			if (!(blockEntities instanceof TrackedWriteMap)) {
				blockEntities = new TrackedWriteMap<>(Diagnostic::illegalBlockEntityStorageAccess, blockEntities);
			}
			if (!(pendingBlockEntities instanceof TrackedWriteMap)) {
				pendingBlockEntities = new TrackedWriteMap<>(Diagnostic::illegalBlockEntityStorageAccess, pendingBlockEntities);
			}
		}
	}
}
