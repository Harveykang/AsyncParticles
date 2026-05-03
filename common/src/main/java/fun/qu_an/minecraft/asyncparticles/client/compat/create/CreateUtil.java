package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CreateUtil {
	@Nullable
	@ExpectPlatform
	public static BlockHitResult clip(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	@ExpectPlatform
	public static ContraptionHitResult clipWithContactPointMotion(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@ExpectPlatform
	public static boolean isUnderContraption(ClientLevel level, Vec3 pos, double size) {
		ExceptionUtil.throwAssertionError();
		return false;
	}

	@ExpectPlatform
	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		ExceptionUtil.throwAssertionError();
		return false;
	}
}
