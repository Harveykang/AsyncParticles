package fun.qu_an.minecraft.asyncparticles.client.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import net.minecraft.ReportedException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;
import java.util.function.Predicate;

import static java.lang.Math.abs;
import static org.joml.Math.max;

public class GameUtil {
	public static final ParticleThreadLocal<Integer> DESTRUCTION_LIGHT_CACHE = new ParticleThreadLocal<>();
	public static final ParticleThreadLocal<BlockPos.MutableBlockPos> SHARED_POS = ParticleThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	@ExpectPlatform
	public static AABB infinityAABB() {
		throw new AssertionError();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(AsyncParticlesClient.MOD_ID, path);
	}

	public static ReportedException getReportedException(Throwable t) {
		while (t != null) {
			if (t instanceof ReportedException re) {
				return re;
			}
			t = t.getCause();
		}
		return null;
	}

	public static double manhattanLength(Vec3 vec3) {
		return abs(vec3.x) + abs(vec3.y) + abs(vec3.z);
	}

	public static Queue<Particle> newParticleQueue() {
		return IterationSafeEvictingQueue.newInstance(
			16,
			ConfigHelper.getParticleLimit(),
			AsyncTickBehavior.INSTANCE::onEvicted);
	}

	public static int getLightColorFromNeighbor(ClientLevel level, BlockPos pos) {
		var asyncparticles$mutable = new BlockPos.MutableBlockPos();
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int lx = x & 15;
		int ly = y & 15;
		int lz = z & 15;
		int i;
		int j;

		LayerLightEventListener sky = level.getLightEngine().getLayerListener(LightLayer.SKY);
		DataLayer skyDataLayerData = sky.getDataLayerData(SectionPos.of(pos));
		if (skyDataLayerData == null) {
			i = 0;
		} else {
			i = ly == 15 ? sky.getLightValue(asyncparticles$mutable.set(x, y + 1, z)) :
				skyDataLayerData.get(lx, ly + 1, lz);
			if (i < 15) {
				i = max(i, lz == 0 ? sky.getLightValue(asyncparticles$mutable.set(x, y, z - 1)) :
					skyDataLayerData.get(lx, ly, lz - 1));
				if (i < 15) {
					i = max(i, lx == 0 ? sky.getLightValue(asyncparticles$mutable.set(x - 1, y, z)) :
						skyDataLayerData.get(lx - 1, ly, lz));
					if (i < 15) {
						i = max(i, lz == 15 ? sky.getLightValue(asyncparticles$mutable.set(x, y, z + 1)) :
							skyDataLayerData.get(lx, ly, lz + 1));
						if (i < 15) {
							i = max(i, lx == 15 ? sky.getLightValue(asyncparticles$mutable.set(x + 1, y, z)) :
								skyDataLayerData.get(lx + 1, ly, lz));
							if (i < 15) {
								i = max(i, ly == 0 ? sky.getLightValue(asyncparticles$mutable.set(x, y - 1, z)) :
									skyDataLayerData.get(lx, ly - 1, lz));
								if (i < 15 && i > 0) {
									--i;
								}
							}
						}
					}
				}
			}
		}

		LayerLightEventListener block = level.getLightEngine().getLayerListener(LightLayer.BLOCK);
		DataLayer blockDataLayerData = block.getDataLayerData(SectionPos.of(pos));
		if (blockDataLayerData == null) {
			j = 15;
		} else {
			j = ly == 15 ? block.getLightValue(asyncparticles$mutable.set(x, y + 1, z)) :
				blockDataLayerData.get(lx, ly + 1, lz);
			if (j < 15) {
				j = max(j, lz == 0 ? block.getLightValue(asyncparticles$mutable.set(x, y, z - 1)) :
					blockDataLayerData.get(lx, ly, lz - 1));
				if (j < 15) {
					j = max(j, lx == 0 ? block.getLightValue(asyncparticles$mutable.set(x - 1, y, z)) :
						blockDataLayerData.get(lx - 1, ly, lz));
					if (j < 15) {
						j = max(j, lz == 15 ? block.getLightValue(asyncparticles$mutable.set(x, y, z + 1)) :
							blockDataLayerData.get(lx, ly, lz + 1));
						if (j < 15) {
							j = max(j, lx == 15 ? block.getLightValue(asyncparticles$mutable.set(x + 1, y, z)) :
								blockDataLayerData.get(lx + 1, ly, lz));
							if (j < 15) {
								j = max(j, ly == 0 ? block.getLightValue(asyncparticles$mutable.set(x, y - 1, z)) :
									blockDataLayerData.get(lx, ly - 1, lz));
							}
						}
					}
				}
			}
		}
		if (j > 0) {
			--j;
		}
		return (i & 0xF) << 20 | (j & 0xF) << 4;
	}

	public static void forEachBlockPos(double x, double y, double z, double size, Predicate<BlockPos> longConsumer) {
		int l = Mth.floor(x - size);
		int m = Mth.floor(x + size);
		int n = Mth.floor(y - size);
		int o = Mth.floor(y + size);
		int p = Mth.floor(z - size);
		int q = Mth.floor(z + size);
		BlockPos.MutableBlockPos pos = GameUtil.SHARED_POS.get();
		if (l == m && n == o && p == q) {
			longConsumer.test(pos.set(l, n, p));
		} else {
			for (int r = l; r <= m; r++) {
				for (int s = n; s <= o; s++) {
					for (int t = p; t <= q; t++) {
						if (!longConsumer.test(pos.set(r, s, t))) {
							return;
						}
					}
				}
			}
		}
	}
}
