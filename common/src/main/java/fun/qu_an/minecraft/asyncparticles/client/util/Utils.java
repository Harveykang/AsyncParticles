package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Unique;

public class Utils {
	public static RuntimeException toThrowDirectly(@NotNull Throwable t) {
		return toThrowDirectly0(t);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T toThrowDirectly0(Throwable t) throws T {
		throw (T) t;
	}

	public static Throwable getRootCause(Throwable e) {
		if (e == null) {
			return null;
		}
		while (true) {
			if (e.getCause() == null) {
				return e;
			}
			e = e.getCause();
		}
	}
}
