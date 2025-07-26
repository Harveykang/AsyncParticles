package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ParticleRainCompat {
	public static final ParticleRainCompat INSTANCE;

	static {
		init0();
		INSTANCE = newInstance();
	}

	public final AtomicInteger particleCount = new AtomicInteger(0);
	public final AtomicInteger fogCount = new AtomicInteger(0);

	public static void init() {
		// init0() is called in the <clinit>
		if (!ModListHelper.IS_LEGACY_PARTICLERAIN) {
			throw new IllegalStateException("Mod ParticleRain is not legacy.");
		}
	}

	private static void init0() {
		if (ModListHelper.VS_LOADED) {
			ParticleRainAddon.Type.RAIN.register((level, location, originalMovement, aabb) -> {
				Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(originalMovement, aabb, level);
				if (shipMovement == null) {
					return originalMovement;
				}
				INSTANCE.onShipCollision(level, location, shipMovement, aabb);
				return shipMovement;
			});
			ParticleRainAddon.CollisionFunction function = (level, location, v, aabb) -> {
				Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(v, aabb, level);
				return shipMovement == null ? v : shipMovement;
			};
			ParticleRainAddon.Type.SNOW.register(function);
			ParticleRainAddon.Type.OTHER.register(function);
		}
		if (ModListHelper.CREATE_LOADED) {
			ParticleRainAddon.Type.RAIN.register((level, position, motion, aabb) -> {
				Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
				if (collide == null) {
					return motion;
				}
				INSTANCE.onCreateCollision(level, motion, collide, aabb);
				return collide;
			});
			ParticleRainAddon.CollisionFunction function = (level, position, motion, aabb) -> {
				Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
				return collide == null ? motion : collide;
			};
			ParticleRainAddon.Type.SNOW.register(function);
			ParticleRainAddon.Type.OTHER.register(function);
		}
	}

	@ExpectPlatform
	private static ParticleRainCompat newInstance() {
		throw new AssertionError();
	}

	public void clearCounters() {
		particleCount.set(0);
		fogCount.set(0);
	}

	public abstract void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb);

	public abstract void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb);
}
