package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public class CreateCompat {
	public static boolean canSpawnWeatherParticle(ClientLevel level, int x, int y, int z) {
		return !CreateUtil.isUnderContraption(level, x, y, z);
	}

	public static boolean canSpawnWeatherParticleFloorToInt(ClientLevel level, double x, double y, double z) {
		return canSpawnWeatherParticle(level, Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}

	public static boolean canSpawnWeatherParticle(ClientLevel level, double x, double y, double z, double size) {
		return !CreateUtil.isUnderContraption(level, x, y, z, size);
	}
}
