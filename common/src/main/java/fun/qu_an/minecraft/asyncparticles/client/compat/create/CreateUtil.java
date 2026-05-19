package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unused")
public class CreateUtil {
	public static final float LENGTH_SQR_EPSILON = 0.001f;

	@ExpectPlatform
	public static Map<Integer, WeakReference<Entity>> loadedContraptions(Level level) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static Collection<WeakReference<Entity>> contraptions(Level level) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static Iterator<Entity> forEachContraption(Level level) {
		throw new AssertionError();
	}

	@Nullable
	@ExpectPlatform
	public static Vec3 getContraptionDeltaMovement(Entity entity) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	public static boolean isUnderContraption(ClientLevel level, Vec3 pos, double size) {
		return isUnderContraption(level, pos.x, pos.y, pos.z, size);
	}

	@ExpectPlatform
	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		ExceptionUtil.throwAssertionError();
		return false;
	}

	@ExpectPlatform
	public static boolean isUnderContraption(ClientLevel level, int x, int y, int z) {
		ExceptionUtil.throwAssertionError();
		return false;
	}
}
