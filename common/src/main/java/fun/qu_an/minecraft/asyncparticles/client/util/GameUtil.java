package fun.qu_an.minecraft.asyncparticles.client.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.ReportedException;
import net.minecraft.world.phys.AABB;

public class GameUtil {
	@ExpectPlatform
	public static AABB infinityAABB() {
		throw new AssertionError();
	}

	public static ReportedException getReportedException(Throwable t) {
		if (t instanceof ReportedException re) {
			return re;
		}
		Throwable cause = t.getCause();
		return cause == null ? null : getReportedException(cause);
	}
}
