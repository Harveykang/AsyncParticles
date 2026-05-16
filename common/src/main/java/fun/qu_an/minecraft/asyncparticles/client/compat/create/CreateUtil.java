package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.utility.VecHelper;
import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create.AccessorMatrix3d;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CreateUtil {
	public static final float LENGTH_SQR_EPSILON = 0.01f;

	public static Map<Integer, WeakReference<?>> loadedContraptions(LevelAccessor level) {
		if (ModListHelper.IS_LEGACY_CREATE) {
			return (Map) ContraptionHandler.loadedContraptions.get(level);
		}
		return loadedContraptions0(level);
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
		GameUtil.forEachBlockPos(x, 0, z, size, (blockPos) -> {
			boolean b = ContraptionRainBlocking.getHeight(level, blockPos) >= y;
			result[0] = b;
			return !b;
		});
		return result[0];
	}

	public static boolean isUnderContraption(ClientLevel level, int x, int y, int z) {
		return ContraptionRainBlocking.getHeight(level, x, z) >= y;
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

	public static org.joml.Matrix3d toJOML(Matrix3d createMatrix) {
		return toJOML(createMatrix, new org.joml.Matrix3d());
	}

	public static org.joml.Matrix3d toJOML(Matrix3d createMatrix, org.joml.Matrix3d jomlMatrix) {
		AccessorMatrix3d accessor = ((AccessorMatrix3d) createMatrix);
		jomlMatrix.set(accessor.m00(),
			accessor.m01(),
			accessor.m02(),
			accessor.m10(),
			accessor.m11(),
			accessor.m12(),
			accessor.m20(),
			accessor.m21(),
			accessor.m22());
		return jomlMatrix;
	}

	public static Matrix3d toCreate(org.joml.Matrix3d jomlMatrix) {
		return toCreate(jomlMatrix, new Matrix3d());
	}

	public static Matrix3d toCreate(org.joml.Matrix3d jomlMatrix, Matrix3d createMatrix) {
		AccessorMatrix3d accessor = ((AccessorMatrix3d) createMatrix);
		accessor.m00(jomlMatrix.m00);
		accessor.m01(jomlMatrix.m01);
		accessor.m02(jomlMatrix.m02);
		accessor.m10(jomlMatrix.m10);
		accessor.m11(jomlMatrix.m11);
		accessor.m12(jomlMatrix.m12);
		accessor.m20(jomlMatrix.m20);
		accessor.m21(jomlMatrix.m21);
		accessor.m22(jomlMatrix.m22);
		return createMatrix;
	}

	public static AABB getBoundingBox(AbstractContraptionEntity contraptionEntity) {
		AABB original = contraptionEntity.getBoundingBox();
		if (ModListHelper.VS_LOADED) {
			original = VSCompat.getBoundingBox(original, contraptionEntity);
		}
		return original;
	}

	public static AABB expandTowards(AbstractContraptionEntity contraptionEntity, AABB aabb, Vec3 expansion) {
		AABB original = aabb.expandTowards(expansion);
		if (ModListHelper.VS_LOADED) {
			original = VSCompat.expandTowards(original, contraptionEntity);
		}
		return original;
	}

	public static Vec3 getAnchorVec(AbstractContraptionEntity contraptionEntity) {
		Vec3 original = contraptionEntity.getAnchorVec();
		if (ModListHelper.VS_LOADED) {
			original = VSCompat.getAnchorVec(original, contraptionEntity);
		}
		return original;
	}

	public static Matrix3d asMatrix(AbstractContraptionEntity contraptionEntity, AbstractContraptionEntity.ContraptionRotationState rotation) {
		Matrix3d original = rotation.asMatrix();
		if (ModListHelper.VS_LOADED) {
			original = VSCompat.asMatrix(original, contraptionEntity);
		}
		return original;
	}

	public static Vec3 getContactPointMotion(AbstractContraptionEntity contraptionEntity, Vec3 contactPoint) {
		Vec3 original = contraptionEntity.getContactPointMotion(contactPoint);
		if (ModListHelper.VS_LOADED) {
			original = VSCompat.getContactPointMotion(original, contraptionEntity, contactPoint);
		}
		return original;
	}

}
