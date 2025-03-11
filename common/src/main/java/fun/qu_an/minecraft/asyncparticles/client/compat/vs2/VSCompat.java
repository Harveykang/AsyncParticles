package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import net.minecraft.client.particle.Particle;

public class VSCompat {
	public static void removeIfOutSight(Particle particle) {
		if (VSClientUtils.isOutSight(particle)) {
			particle.remove();
		}
	}
}
