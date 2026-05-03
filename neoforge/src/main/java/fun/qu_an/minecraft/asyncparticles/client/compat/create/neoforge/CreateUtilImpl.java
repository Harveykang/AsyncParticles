package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionHitResult;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.max;

@SuppressWarnings("unused")
public class CreateUtilImpl {
	public static Map<Integer, WeakReference<?>> loadedContraptions(LevelAccessor level) {
		return (Map) ContraptionHandler.loadedContraptions.get(level);
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

	@Nullable
	public static BlockHitResult clip(ClientLevel level, Vec3 start, Vec3 end) {
		double shortestDistance = Double.MAX_VALUE;
		BlockHitResult hitResult = null;
		Vec3 hit = null;
		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity1 = it.next();
			AABB entity1Bb = entity1.getBoundingBox();
			if (!entity1Bb.intersects(start, end)) {
				continue;
			}
			BlockHitResult hitResult1 = ContraptionHandlerClient.rayTraceContraption(start, end, entity1);
			if (hitResult1 != null) {
				Vec3 hit1 = entity1.toGlobalVector(hitResult1.getLocation(), 1.0F);
				double hitDiff = start.y - hit1.y;
				if (shortestDistance > hitDiff) {
					hitResult = hitResult1;
					hit = hit1;
				}
			}
		}
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return null;
		}
		return new BlockHitResult(hit,
			hitResult.getDirection(),
			BlockPos.containing(hit),
			hitResult.isInside());
	}

	@Nullable
	public static ContraptionHitResult clipWithContactPointMotion(ClientLevel level, Vec3 start, Vec3 end) {
		double shortestDistance = Double.MAX_VALUE;
		BlockHitResult hitResult = null;
		Vec3 hit = null;
		AbstractContraptionEntity entity = null;
		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity1 = it.next();
			AABB bb0;
			AABB entity1Bb = entity1 instanceof CarriageContraptionEntity
				? (bb0 = entity1.getBoundingBox()).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
				: entity1.getBoundingBox();
			if (!entity1Bb.intersects(start, end)) {
				continue;
			}
			BlockHitResult hitResult1 = ContraptionHandlerClient.rayTraceContraption(start, end, entity1);
			if (hitResult1 != null) {
				Vec3 hit1 = entity1.toGlobalVector(hitResult1.getLocation(), 1.0F);
				double hitDiff = start.y - hit1.y;
				if (shortestDistance > hitDiff) {
					hitResult = hitResult1;
					hit = hit1;
					entity = entity1;
				}
			}
		}
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return null;
		}
		return new ContraptionHitResult(entity.getContactPointMotion(hit),
			hit,
			hitResult.getDirection(),
			BlockPos.containing(hit),
			hitResult.isInside());
	}

	public static boolean isUnderContraption(ClientLevel level, Vec3 pos, double size) {
		return isUnderContraption(level, pos.x, pos.y, pos.z, size);
	}

	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		Long2FloatMap heightMap = ContraptionRainBlocking.getAttachedContractionHeightMap(level);
		Long2BooleanMap movingMap = ContraptionRainBlocking.getAttachedContractionMovingMap(level);
		boolean[] result = new boolean[1];
		GameUtil.forEachBlockPos(x, 0, z, size, (l) -> {
			boolean b = ContraptionRainBlocking.getHeight(level, l) >= y;
			result[0] = b;
			return !b;
		});
		return result[0];
	}
}
