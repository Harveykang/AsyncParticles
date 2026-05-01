package fun.qu_an.minecraft.asyncparticles.client.core;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import net.minecraft.ReportedException;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class GameUtil {
	@ExpectPlatform
	public static AABB infinityAABB() {
		throw new AssertionError();
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
}
