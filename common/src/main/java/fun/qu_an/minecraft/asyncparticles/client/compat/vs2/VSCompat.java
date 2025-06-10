package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import org.valkyrienskies.mod.util.BugFixUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class VSCompat {
	public static boolean canCreateWeatherParticle(ClientLevel level, double x, double y, double z) {
		return !VSClientUtils.isUnderShipHeightMap(level, x, y, z, 0.5);
	}

	public static boolean canCreateWeatherParticle(ClientLevel level, double x, double y, double z, double size) {
		return !VSClientUtils.isUnderShipHeightMap(level, x, y, z, size);
	}

	private static final MethodHandle isCollisionBoxTooBig;

	static {
		if (ModListHelper.IS_LEGACY_VS) {
			isCollisionBoxTooBig = null;
		} else {
			try {
				Method method = BugFixUtil.INSTANCE.getClass().getMethod("isCollisionBoxTooBig", AABB.class);
				isCollisionBoxTooBig = MethodHandles.lookup().unreflect(method).bindTo(BugFixUtil.INSTANCE);
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static boolean isCollisionBoxTooBig(AABB aabb) {
		if (ModListHelper.IS_LEGACY_VS) {
			return BugFixUtil.INSTANCE.isCollisionBoxToBig(aabb);
		} else {
			try {
				return (boolean) isCollisionBoxTooBig.invokeExact(aabb);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
