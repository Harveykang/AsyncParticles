package fun.qu_an.minecraft.asyncparticles.client.util;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTicker;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;

import static java.lang.Math.abs;

public class GameUtil {
	public static ResourceLocation id(String path) {
		return new ResourceLocation(AsyncParticlesClient.MOD_ID, path);
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

	public static <T extends Particle> Queue<T> newParticleQueue() {
		return IterationSafeEvictingQueue.newInstance(
			16,
			ConfigHelper.getParticleLimit(),
			AsyncTicker::onEvicted);
	}
}
