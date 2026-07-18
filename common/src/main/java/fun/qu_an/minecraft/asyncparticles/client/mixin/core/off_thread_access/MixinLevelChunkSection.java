package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelChunkSection.class)
public class MixinLevelChunkSection {
	@WrapMethod(method = "getBlockState")
	public BlockState getBlockState(int x, int y, int z, Operation<BlockState> original) {
		BlockState call;
		try {
			call = original.call(x, y, z);
		} catch (MissingPaletteEntryException | NullPointerException e) {
			if (ThreadUtil.isOnParticleThread()) {
				return Blocks.AIR.defaultBlockState();
			}
			throw e;
		}
		if (call == null && ThreadUtil.isOnParticleThread()) {
			return Blocks.AIR.defaultBlockState();
		}
		return call;
	}
}
