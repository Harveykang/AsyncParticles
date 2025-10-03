package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LevelChunk.class, priority = 1500)
public abstract class MixinLevelChunk_BlockEntityMap extends ChunkAccess {
	@Shadow
	@Final
	Level level;

	public MixinLevelChunk_BlockEntityMap(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, long l, @Nullable LevelChunkSection[] levelChunkSections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, palettedContainerFactory, l, levelChunkSections, blendingData);
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit1(CallbackInfo ci) {
		if (level.isClientSide()) {
			if (!(blockEntities instanceof ConcurrentHashMap)) {
				blockEntities = new ConcurrentHashMap<>(blockEntities);
			}
			if (!(pendingBlockEntities instanceof ConcurrentHashMap)) {
				pendingBlockEntities = new ConcurrentHashMap<>(pendingBlockEntities);
			}
		}
	}

	@Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
		at = @At("HEAD"), order = 500, cancellable = true)
	private void onGetBlockEntity(BlockPos pos,
								  LevelChunk.EntityCreationType creationType,
								  CallbackInfoReturnable<BlockEntity> cir) {
		if (level.isClientSide() && ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(blockEntities.get(pos));
		}
	}
}
