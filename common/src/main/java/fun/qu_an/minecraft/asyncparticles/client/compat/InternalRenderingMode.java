package fun.qu_an.minecraft.asyncparticles.client.compat;

import fun.qu_an.minecraft.asyncparticles.client.compat.iris.IrisCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.RenderingMode;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import org.jetbrains.annotations.ApiStatus;

public class InternalRenderingMode {
	public static final int
		DELAYED_ASYNC = 1, // RenderingMode.DELAYED
		COMPATIBILITY_ASYNC = 3, // RenderingMode.COMPATIBILITY
		IRIS_BEFORE_ASYNC = 5,
		IRIS_MIXED_ASYNC = 7,
		SYNC = 0, // RenderingMode.SYNCHRONOUSLY
		IRIS_BEFORE_SYNC = 2,
		IRIS_MIXED_SYNC = 4;
	private static int mode = 0;

	@ApiStatus.Internal
	public static int updateInternalMode(RenderingMode renderingMode) {
		if (!ModListHelper.IRIS_LIKE_LOADED) {
			return mode = switch (renderingMode) {
				case SYNCHRONOUSLY -> SYNC;
				case DELAYED -> DELAYED_ASYNC;
				case COMPATIBILITY -> COMPATIBILITY_ASYNC;
			};
		}
		ParticleRenderingSettings settings = IrisCompat.getParticleRenderingSettings();
		int i = (settings.ordinal() << 2) | renderingMode.ordinal(); // UNSET.ordinal() == 0
		return mode = switch (i) {
			case 0, 0b1100 -> SYNC; // IRIS_SYNC_AFTER
			case 1, 0b1101 -> DELAYED_ASYNC; // IRIS_ASYNC_AFTER
			case 2, 0b1110 -> COMPATIBILITY_ASYNC; // IRIS_ASYNC_AFTER
			case 0b100 -> IRIS_BEFORE_SYNC;
			case 0b101, 0b110 -> IRIS_BEFORE_ASYNC;
			case 0b1000 -> IRIS_MIXED_SYNC;
			case 0b1001, 0b1010 -> IRIS_MIXED_ASYNC;
			default -> throw new IllegalStateException("Unexpected value: " + i);
		};
	}

	public static int getMode() {
		return mode;
	}

	public static boolean isDelayed() {
		return mode == DELAYED_ASYNC;
	}

	public static boolean isASync() {
		return (mode & 1) != 0;
	}

	public static boolean isSync() {
		return (mode & 1) == 0;
	}
}
