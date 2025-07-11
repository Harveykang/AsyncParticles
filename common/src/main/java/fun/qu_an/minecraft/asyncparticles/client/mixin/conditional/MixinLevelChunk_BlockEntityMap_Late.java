package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LevelChunk.class, priority = 1500)
public abstract class MixinLevelChunk_BlockEntityMap_Late extends ChunkAccess {
	@Shadow
	@Final
	Level level;

	public MixinLevelChunk_BlockEntityMap_Late(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit1(CallbackInfo ci) {
		if (level.isClientSide) {
			if (!(blockEntities instanceof ConcurrentHashMap)) {
				blockEntities = new ConcurrentHashMap<>(blockEntities);
			}
			if (!(pendingBlockEntities instanceof ConcurrentHashMap)) {
				pendingBlockEntities = new ConcurrentHashMap<>(pendingBlockEntities);
			}
		}
	}

}
