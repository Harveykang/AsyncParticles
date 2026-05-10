package fun.qu_an.minecraft.asyncparticles.client.compat.create.fabric;

import com.simibubi.create.content.contraptions.ContraptionHandler;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Map;

@SuppressWarnings("unused")
public class CreateUtilImpl {
	public static Map<Integer, WeakReference<?>> loadedContraptions0(LevelAccessor level) {
		return (Map) ContraptionHandler.loadedContraptions.get(level);
	}

	public static Vec3 vecRotate0(Vec3 worldMin, float yawOffset, Direction.Axis axis) {
		return VecHelper.rotate(worldMin, -yawOffset, axis);
	}
}
