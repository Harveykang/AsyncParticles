package fun.qu_an.minecraft.asyncparticles.client.core;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.Platform;
import net.minecraft.ReportedException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.phys.AABB;

import static org.joml.Math.max;

public class GameUtil {
	public static AABB infinityAABB() {
		return Platform.PLATFORM.infinityAABB();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(AsyncParticlesClient.MOD_ID, path);
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
}
