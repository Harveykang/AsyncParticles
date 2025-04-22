package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.minecraft.client.multiplayer.ClientLevel;

public class CreateCompat {
	public static boolean canSpawnWeatherParticle(ClientLevel level, double x, double y, double z) {
		return !CreateUtil.isUnderContraption(level, x, y, z);
	}
}
