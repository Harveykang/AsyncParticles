package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface WeatherParticleAddon {
	Map<Type, CollisionFunction> collisionFunctions = new ConcurrentHashMap<>();

	AABB asyncparticles$getWeatherAABB();

	void asyncparticles$setWeatherAABB(AABB aabb);

	boolean asyncparticles$invisible();

	void asyncparticles$setInvisible(boolean visible);

	static CollisionFunction asyncParticles$getCollisionFunction(Type type) {
		return collisionFunctions.getOrDefault(type, (level, location, v, aabb) -> v);
	}

	static void asyncParticles$registerCollisionFunction(Type type, CollisionFunction function) {
		CollisionFunction function1 = collisionFunctions.get(type);
		if (function1 != null) {
			function = function1.andThen(function);
		}
		collisionFunctions.put(type, function);
	}

	@FunctionalInterface
	interface CollisionFunction {
		/**
		 * @return null if the particle should be removed, otherwise the motion vector
		 */
		@Nullable Vec3 apply(@NotNull ClientLevel level, @NotNull Vec3 location, @NotNull Vec3 motion, @NotNull AABB aabb);

		default CollisionFunction andThen(CollisionFunction function) {
			return (level, location, v, aabb) -> {
				Vec3 apply = this.apply(level, location, v, aabb);
				if (apply == null) {
					return null;
				}
				return function.apply(level, location, apply, aabb);
			};
		}
	}

	enum Type {
		RAIN,
		SNOW,
		OTHER;
		private CollisionFunction function;

		/**
		 * @return null if the particle should be removed, otherwise the motion vector
		 */
		public @Nullable Vec3 collide(@NotNull ClientLevel level, @NotNull Vec3 position, @NotNull Vec3 motion, @NotNull AABB aabb) {
			return function == null ? motion : function.apply(level, position, motion, aabb);
		}

		public void register(CollisionFunction function) {
			CollisionFunction function1 = this.function;
			if (function1 == null) {
				this.function = function;
			} else {
				this.function = function1.andThen(function);
			}
		}
	}
}
