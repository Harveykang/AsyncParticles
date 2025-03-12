package fun.qu_an.minecraft.asyncparticles.client.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.HashMap;

@Mixin(ChunkAccess.class)
public class MixinChunkAccess {
	@Redirect(method = "<init>",
		slice = @Slice(
			from = @At(value = "FIELD", shift = At.Shift.BEFORE, target = "Lnet/minecraft/world/level/chunk/ChunkAccess;blockEntities:Ljava/util/Map;")
		),
		at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"))
	private <K, V> HashMap<K, V> newHashMap() {
		return (Object) this instanceof LevelChunk ? null : new HashMap<>();
	}
}
