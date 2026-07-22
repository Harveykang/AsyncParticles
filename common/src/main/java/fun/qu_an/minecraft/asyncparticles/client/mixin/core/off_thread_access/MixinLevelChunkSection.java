package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelChunkSection.class)
public class MixinLevelChunkSection {
	@WrapMethod(method = "getBlockState")
	public BlockState onGetBlockState(int x, int y, int z, Operation<BlockState> original) {
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

	@WrapMethod(method = "getFluidState")
	public FluidState onGetFluidState(int x, int y, int z, Operation<FluidState> original) {
		FluidState call;
		try {
			call = original.call(x, y, z);
		} catch (MissingPaletteEntryException | NullPointerException e) {
			if (ThreadUtil.isOnParticleThread()) {
				return Fluids.EMPTY.defaultFluidState();
			}
			throw e;
		}
		if (call == null && ThreadUtil.isOnParticleThread()) {
			return Fluids.EMPTY.defaultFluidState();
		}
		return call;
	}

	@WrapMethod(method = "getNoiseBiome")
	public Holder<Biome> onGetNoiseBiome(int x, int y, int z, Operation<Holder<Biome>> original) {
		Holder<Biome> call;
		try {
			call = original.call(x, y, z);
		} catch (MissingPaletteEntryException | NullPointerException e) {
			if (ThreadUtil.isOnParticleThread()) {
				// assert client environment
				return Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.BIOME)
					.getHolderOrThrow(Biomes.PLAINS);
			}
			throw e;
		}
		if (call == null && ThreadUtil.isOnParticleThread()) {
			// assert client environment
			return Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.BIOME)
				.getHolderOrThrow(Biomes.PLAINS);
		}
		return call;
	}
}
