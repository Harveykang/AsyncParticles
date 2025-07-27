package fun.qu_an.minecraft.asyncparticles.client.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class GameUtil {
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
}
