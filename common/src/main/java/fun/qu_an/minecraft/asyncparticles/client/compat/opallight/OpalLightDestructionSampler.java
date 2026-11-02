package fun.qu_an.minecraft.asyncparticles.client.compat.opallight;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import org.mesdag.opallight.light.color.LightColorData;
import org.mesdag.opallight.light.color.LightColorSampler;
import org.mesdag.opallight.light.engine.LightColorCache;

public class OpalLightDestructionSampler implements BlockGetter, LightColorSampler.Colors {
	private static final Direction[] DIRECTIONS = Direction.values();
	ClientLevel level;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
	private BlockState replacement;
	private final long[] colors = new long[27];
	private final float[] sampled = new float[3];
	private final LightColorSampler sampler = new LightColorSampler(this, true);

	void prepare(ClientLevel level, BlockPos pos, BlockState destroyedState, LightColorCache cache) {
		this.level = level;
		this.pos.set(pos.getX(), pos.getY(), pos.getZ());
		BlockState current = level.getBlockState(pos);
		this.replacement = current == destroyedState ? destroyedState.getFluidState().createLegacyBlock() : current;
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					samplePos.set(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
					long color = 0L;
					if (!level.isOutsideBuildHeight(samplePos) && level.hasChunkAt(samplePos)) {
						Long2LongOpenHashMap section = cache.getSection(SectionPos.asLong(samplePos.getX() >> 4, samplePos.getY() >> 4, samplePos.getZ() >> 4));
						color = section == null ? 0L : section.get(samplePos.asLong());
					}
					colors[x * 9 + y * 3 + z] = color;
				}
			}
		}
		if (colors[13] == 0L && replacement.getLightBlock(this, pos) < 15) {
			colors[13] = predictCenter(samplePos);
		}
	}

	void clear() {
		sampler.clear();
		level = null;
		replacement = null;
	}

	public int sample(double x, double y, double z) {
		if (Mth.floor(x) != pos.getX() || Mth.floor(y) != pos.getY() || Mth.floor(z) != pos.getZ()) {
			return 0;
		}
		sampler.sample(this, x, y, z, pos.getX(), pos.getY(), pos.getZ(), sampled);
		return Math.round(sampled[0] * 255) << 16 | Math.round(sampled[1] * 255) << 8 | Math.round(sampled[2] * 255);
	}

	@Override
	public long get(int x, int y, int z) {
		int dx = x - pos.getX() + 1, dy = y - pos.getY() + 1, dz = z - pos.getZ() + 1;
		return dx < 0 || dx > 2 || dy < 0 || dy > 2 || dz < 0 || dz > 2 ? 0L : colors[dx * 9 + dy * 3 + dz];
	}

	private long predictCenter(BlockPos.MutableBlockPos neighbor) {
		float red = 0, green = 0, blue = 0, strength = 0;
		for (Direction direction : DIRECTIONS) {
			neighbor.setWithOffset(pos, direction);
			long color = get(neighbor.getX(), neighbor.getY(), neighbor.getZ());
			if (color == 0L) {
				continue;
			}
			BlockState adjacent = getBlockState(neighbor);
			if (adjacent.getLightBlock(this, neighbor) >= 15 || Shapes.faceShapeOccludes(
				LightEngine.getOcclusionShape(this, pos, replacement, direction),
				LightEngine.getOcclusionShape(this, neighbor, adjacent, direction.getOpposite()))) {
				continue;
			}
			float r = LightColorData.channel(color, 32), g = LightColorData.channel(color, 16), b = LightColorData.channel(color, 0);
			red += r;
			green += g;
			blue += b;
			strength = Math.max(strength, Math.max(r, Math.max(g, b)));
		}
		float peak = Math.max(red, Math.max(green, blue));
		float scale = peak == 0 ? 0 : strength * (14f / 15f) / peak;
		return LightColorData.pack(red * scale, green * scale, blue * scale);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return this.pos.equals(pos) ? replacement : level.getBlockState(pos);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return this.pos.equals(pos) ? null : level.getBlockEntity(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return level.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return level.getMinBuildHeight();
	}
}
