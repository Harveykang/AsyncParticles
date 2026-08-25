package fun.qu_an.minecraft.asyncparticles.client.util;

import net.minecraft.ReportedException;
import org.jetbrains.annotations.NotNull;

public class ExceptionUtil {
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

	public static void throwAssertionError() {
		throw new AssertionError();
	}
}
