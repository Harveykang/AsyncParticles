package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public class VSCompat {
	public static void removeIfOutSight(Particle particle) {
		if (VSClientUtils.isOutSight(particle)) {
			particle.remove();
		}
	}

	public static boolean canCreateWeatherParticle(ClientLevel level, double x, double y, double z) {
		return !VSClientUtils.isUnderShipHeightMap(level, x, y, z);
	}
}
