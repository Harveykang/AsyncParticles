package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.foundation.utility.VecHelper;
import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CreateUtil {
	public static Map<Integer, WeakReference<?>> loadedContraptions(LevelAccessor level) {
		if (ModListHelper.IS_LEGACY_CREATE){
			return (Map) ContraptionHandler.loadedContraptions.get(level);
		}
		return loadedContraptions0( level);
	}

	@ExpectPlatform
	private static Map<Integer, WeakReference<?>> loadedContraptions0(LevelAccessor level) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	public static Collection<WeakReference<?>> contraptions(LevelAccessor level) {
		return loadedContraptions(level).values();
	}

	public static Iterator<AbstractContraptionEntity> forEachContraption(LevelAccessor level) {
		Iterator<WeakReference<AbstractContraptionEntity>> iterator = (Iterator) contraptions(level).iterator();
		return new Iterator<>() {
			private AbstractContraptionEntity next;

			@Override
			public boolean hasNext() {
				if (next != null) {
					return true;
				}
				while (iterator.hasNext()) {
					try {
						if ((next = iterator.next().get()) == null) {
							continue;
						}
					} catch (ConcurrentModificationException ignored) {
						// Ignore as they are not critical
						next = null;
						return false;
					}
					if (next.isAliveOrStale()) {
						return true;
					}
				}
				next = null;
				return false;
			}

			@Override
			public AbstractContraptionEntity next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				AbstractContraptionEntity result = next;
				next = null;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void forEachRemaining(Consumer<? super AbstractContraptionEntity> action) {
				while (hasNext()) {
					action.accept(next);
					next = null;
				}
			}
		};
	}

	@Nullable
	public static Vec3 getContraptionDeltaMovement(Entity entity) {
		Entity rootEntity = entity.getRootVehicle();
		if (rootEntity instanceof AbstractContraptionEntity ace) {
			return ace.getContactPointMotion(entity.position());
		}
		for (Iterator<AbstractContraptionEntity> iterator = forEachContraption(rootEntity.level()); iterator.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = iterator.next();
			if (contraptionEntity.collidingEntities.containsKey(rootEntity)) {
				return contraptionEntity.getContactPointMotion(entity.position());
			}
		}
		return null;
	}

	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		boolean[] result = new boolean[1];
		GameUtil.forEachBlockPos(x, 0, z, size, (l) -> {
			boolean b = ContraptionRainBlocking.getHeight(level, l) >= y;
			result[0] = b;
			return !b;
		});
		return result[0];
	}

	public static boolean isUnderContraption(ClientLevel level, int x, int y, int z) {
		return ContraptionRainBlocking.getHeight(level, BlockPos.asLong(x, 0, z)) >= y;
	}

	public static Vec3 vecRotate(Vec3 worldMin, float yawOffset, Direction.Axis axis) {
		if (ModListHelper.IS_LEGACY_CREATE) {
			return VecHelper.rotate(worldMin, yawOffset, axis);
		}
		return vecRotate0(worldMin, yawOffset, axis);
	}

	@ExpectPlatform
	private static Vec3 vecRotate0(Vec3 worldMin, float yawOffset, Direction.Axis axis) {
		ExceptionUtil.throwAssertionError();
		return null;
	}
}
