package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ApiStatus.Internal
public class CreateUtilImpl {
	public static Map<Integer, WeakReference<Entity>> loadedContraptions(LevelAccessor level) {
		return (Map) ContraptionHandler.loadedContraptions.get(level);
	}

	public static Collection<WeakReference<Entity>> contraptions(LevelAccessor level) {
		return CreateUtil.loadedContraptions(level).values();
	}

	public static Iterator<Entity> forEachContraption(LevelAccessor level) {
		Iterator<WeakReference<AbstractContraptionEntity>> iterator = (Iterator) CreateUtil.contraptions(level).iterator();
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
			public void forEachRemaining(Consumer<? super Entity> action) {
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
		for (Iterator<Entity> iterator = forEachContraption(rootEntity.level()); iterator.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) iterator.next();
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
}
