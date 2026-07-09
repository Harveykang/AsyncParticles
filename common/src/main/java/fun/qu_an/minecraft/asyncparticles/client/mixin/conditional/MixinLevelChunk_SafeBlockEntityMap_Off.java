package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedWriteHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelChunk.class)
public abstract class MixinLevelChunk_SafeBlockEntityMap_Off extends ChunkAccess {
	@Shadow
	@Final
	private Level level;
	@Unique
	private static boolean asyncparticles$accessedOffThread = false;

	public MixinLevelChunk_SafeBlockEntityMap_Off(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}

	@Unique
	private static void asyncparticles$alert() {
		if (ThreadUtil.isOnParticleThread() && !asyncparticles$accessedOffThread) {
			LogUtils.getLogger().warn("Block entity storage accessed off the main thread!\nConsider enabling 'safeBlockEntityMap'.", new IllegalStateException(""));
			ThreadUtil.enqueueClientTask(() -> {
				Minecraft.getInstance().gui.getChat().addMessage(
					Component.translatable("chat.asyncparticles.warn.get_block_entity_off_main_thread",
							Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap"))
						.withStyle(ChatFormatting.DARK_RED)
				);
			});
			asyncparticles$accessedOffThread = true;
		}
	}

	@Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
		at = @At("HEAD"), cancellable = true)
	private void onGetBlockEntity(BlockPos pos,
	                              LevelChunk.EntityCreationType creationType,
	                              CallbackInfoReturnable<BlockEntity> cir) {
		if (level.isClientSide && ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(blockEntities.get(pos));
		}
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit1(CallbackInfo ci) {
		if (level.isClientSide) {
			if (!(blockEntities instanceof TrackedWriteHashMap)) {
				blockEntities = new TrackedWriteHashMap<>(() -> asyncparticles$alert(), blockEntities);
			}
			if (!(pendingBlockEntities instanceof TrackedWriteHashMap)) {
				pendingBlockEntities = new TrackedWriteHashMap<>(() -> asyncparticles$alert(), pendingBlockEntities);
			}
		}
	}
}
