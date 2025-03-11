package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

public class Utils {
	public static RuntimeException toThrowDirectly(@NotNull Throwable t) {
		return toThrowDirectly0(t);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T toThrowDirectly0(Throwable t) throws T {
		throw (T) t;
	}
}
