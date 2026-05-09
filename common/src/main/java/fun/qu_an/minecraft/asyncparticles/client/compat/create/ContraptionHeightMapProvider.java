package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;

public interface ContraptionHeightMapProvider {
	Long2FloatMap asyncparticles$getHeightMap();
	Long2BooleanMap asyncparticles$getMovingMap();
}
