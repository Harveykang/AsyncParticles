package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerEntityShipCollisionUtils;
import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerRaycastUtils;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.apigame.collision.ConvexPolygonc;
import org.valkyrienskies.core.apigame.collision.EntityPolygonCollider;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClient;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import java.util.List;

import static net.minecraft.util.Mth.floor;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

/**
 * See {@link EntityShipCollisionUtils}<p>
 * See {@link VSGameUtilsKt}<p>
 * Make sure our methods behave same as these.
 */
public class VSClientUtils {
	public static Iterable<ClientShip> getShipsInAABB(ClientLevel level, Vector3d v1, Vector3d v2) {
		return getShipsInAABB(level, new AABBd(v1, v2));
	}

	public static Iterable<ClientShip> getShipsInAABB(ClientLevel world, double x1, double y1, double z1, double x2, double y2, double z2) {
		return getShipsInAABB(world, new AABBd(x1, y1, z1, x2, y2, z2));
	}

	public static Iterable<ClientShip> getShipsInAABB(ClientLevel world, AABBd aabb) {
		return VSGameUtilsKt.getShipObjectWorld(world).getLoadedShips().getIntersecting(aabb.correctBounds());
	}

	/**
	 * include matrices in hit result.
	 */
	public static BlockHitResult clipIncludeShips(ClientLevel level, ClipContext ctx, boolean shouldTransformHitPos) {
		var vanillaHit = InvokerRaycastUtils.invoker_vanillaClip(level, ctx);
		var shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
		var closestHit = vanillaHit;
		var closestHitPos = vanillaHit.getLocation();
		var closestHitDist = closestHitPos.distanceToSqr(ctx.getFrom());
		AABBd clipAABB = new AABBd(toJOML(ctx.getFrom()), toJOML(ctx.getTo())).correctBounds();
		for (var ship : shipObjectWorld.getLoadedShips().getIntersecting(clipAABB)) {
			Matrix4dc worldToShip;
			Matrix4dc shipToWorld;
			if (ship instanceof ShipObjectClient soc) {
				worldToShip = soc.getRenderTransform().getWorldToShipMatrix();
				shipToWorld = soc.getRenderTransform().getShipToWorldMatrix();
			} else {
				worldToShip = ship.getWorldToShip();
				shipToWorld = ship.getShipToWorld();
			}

			var shipStart = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getFrom())));
			var shipEnd = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getTo())));

			var shipHit = InvokerRaycastUtils.invoker_clip(level, ctx, shipStart, shipEnd);
			var shipHitPos = toMinecraft(shipToWorld.transformPosition(toJOML(shipHit.getLocation())));
			var shipHitDist = shipHitPos.distanceToSqr(ctx.getFrom());

			if (shipHitDist < closestHitDist && shipHit.getType() != HitResult.Type.MISS) {
				closestHit = ShipHitResult.of(shipHit, worldToShip, shipToWorld);
				closestHitPos = shipHitPos;
				closestHitDist = shipHitDist;
			}
		}
		if (shouldTransformHitPos) {
			closestHit.location = closestHitPos;
		}
		return closestHit;
	}

	private static final EntityPolygonCollider collider = ValkyrienSkiesMod.vsCore.getEntityPolygonCollider();

	/**
	 * No vanilla collision check.
	 * get ship
	 */
	public static Pair<Vec3, ClientShip> entityMovColShipOnlyAndGet(
		@Nullable Entity entity,
		Vec3 movement,
		AABB entityBoundingBox,
		ClientLevel world) {
		// Inflate the bounding box more for players than other entities, to give players a better collision result.
		// Note that this increases the cost of doing collision, so we only do it for the players
		double inflation = (entity instanceof Player) ? 0.5 : 0.1;
		double stepHeight = (entity != null) ? entity.maxUpStep() : 0.0;
		// Add [max(stepHeight - inflation, 0.0)] to search for polygons we might collide with while stepping
		double yMovement = movement.y() + Math.max(stepHeight - inflation, 0.0);
		List<ConvexPolygonc> collidingShipPolygons =
			((InvokerEntityShipCollisionUtils) (Object) EntityShipCollisionUtils.INSTANCE).invoker_getShipPolygonsCollidingWithEntity(
				entity, new Vec3(movement.x(), yMovement, movement.z()),
				entityBoundingBox.inflate(inflation), world);
		if (collidingShipPolygons.isEmpty()) {
			return new Pair<>(movement, null);
		}

		Pair<Vector3dc, Long> pair = collider.adjustEntityMovementForPolygonCollisions(
			toJOML(movement), toJOML(entityBoundingBox), stepHeight, collidingShipPolygons);
		Vector3dc newMovement = pair.getFirst();
		Long shipCollidingWith = pair.getSecond();

		if (shipCollidingWith != null) {
			if (entity != null) {
				// Update the [IEntity.lastShipStoodOn]
				((IEntityDraggingInformationProvider) entity).getDraggingInformation().setLastShipStoodOn(shipCollidingWith);
			}
			return new Pair<>(toMinecraft(newMovement), VSGameUtilsKt.getShipObjectWorld(world).getAllShips().getById(shipCollidingWith));
		}
		return new Pair<>(toMinecraft(newMovement), null);
	}

	/**
	 * No vanilla collision check.
	 */
	@Nullable
	public static Vec3 entityMovColShipOnly(
		@Nullable Entity entity,
		Vec3 movement,
		AABB entityBoundingBox,
		ClientLevel world,
		double inflation) {
		// Inflate the bounding box more for players than other entities, to give players a better collision result.
		// Note that this increases the cost of doing collision, so we only do it for the players
		double stepHeight = (entity != null) ? entity.maxUpStep() : 0.0;
		// Add [max(stepHeight - inflation, 0.0)] to search for polygons we might collide with while stepping
		double yMovement = movement.y() + Math.max(stepHeight - inflation, 0.0);
		List<ConvexPolygonc> collidingShipPolygons =
			((InvokerEntityShipCollisionUtils) (Object) EntityShipCollisionUtils.INSTANCE).invoker_getShipPolygonsCollidingWithEntity(
				entity, new Vec3(movement.x(), yMovement, movement.z()),
				entityBoundingBox.inflate(inflation), world);
		if (collidingShipPolygons.isEmpty()) {
			return null;
		}

		Pair<Vector3dc, Long> pair = collider.adjustEntityMovementForPolygonCollisions(
			toJOML(movement), toJOML(entityBoundingBox), stepHeight, collidingShipPolygons);
		Vector3dc newMovement = pair.getFirst();
		Long shipCollidingWith = pair.getSecond();

		if (shipCollidingWith != null) {
			if (entity != null) {
				// Update the [IEntity.lastShipStoodOn]
				((IEntityDraggingInformationProvider) entity).getDraggingInformation().setLastShipStoodOn(shipCollidingWith);
			}
			return toMinecraft(newMovement);
		}
		return null;
	}

	/**
	 * No vanilla collision check.
	 */
	@Nullable
	public static Vec3 entityMovColShipOnly(
		@Nullable Entity entity,
		Vec3 movement,
		AABB entityBoundingBox,
		ClientLevel world) {
		double inflation = (entity instanceof Player) ? 0.5 : 0.1;
		return entityMovColShipOnly(entity, movement, entityBoundingBox, world, inflation);
	}

	/**
	 * include matrices in hit result.
	 * No vanilla hit result.
	 */
	public static ShipHitResult clipShip(ClientLevel level, ClipContext ctx, boolean shouldTransformHitPos) {
		var shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
		ShipHitResult closestHit = null;
		Vec3 closestHitPos = null;
		double closestHitDist = Double.MAX_VALUE;
		AABBd clipAABB = new AABBd(toJOML(ctx.getFrom()), toJOML(ctx.getTo())).correctBounds();
		for (var ship : shipObjectWorld.getLoadedShips().getIntersecting(clipAABB)) {
			Matrix4dc worldToShip;
			Matrix4dc shipToWorld;
			if (ship instanceof ShipObjectClient soc) {
				worldToShip = soc.getRenderTransform().getWorldToShipMatrix();
				shipToWorld = soc.getRenderTransform().getShipToWorldMatrix();
			} else {
				worldToShip = ship.getWorldToShip();
				shipToWorld = ship.getShipToWorld();
			}

			var shipStart = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getFrom())));
			var shipEnd = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getTo())));

			var shipHit = InvokerRaycastUtils.invoker_clip(level, ctx, shipStart, shipEnd);
			var shipHitPos = toMinecraft(shipToWorld.transformPosition(toJOML(shipHit.getLocation())));
			var shipHitDist = shipHitPos.distanceToSqr(ctx.getFrom());

			if (shipHitDist < closestHitDist && shipHit.getType() != HitResult.Type.MISS) {
				closestHit = ShipHitResult.of(shipHit, worldToShip, shipToWorld);
				closestHitPos = shipHitPos;
				closestHitDist = shipHitDist;
			}
		}
		//noinspection ConstantValue
		if (shouldTransformHitPos && closestHit != null) {
			closestHit.location = closestHitPos;
		}
		return closestHit;
	}

	public static boolean isUnderHeightMapIncludeShips(ClientLevel level, double x, double y, double z) {
		if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor(x), floor(z)) >= y) {
			return true;
		}
		return isUnderShipHeightMap(level, x, y, z);
	}

	public static boolean isUnderShipHeightMap(ClientLevel level, double x, double y, double z) {
		var shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
		for (var nearbyShip : shipObjectWorld.getAllShips().getIntersecting(
			new AABBd(x - 1, y - 1, z - 1, x + 1, Math.max(y + 16, level.getMaxBuildHeight()), z + 1))) {
			var posInShip = nearbyShip.getWorldToShip().transformPosition(new Vector3d(x, y, z));
			if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor(posInShip.x), floor(posInShip.z)) + 1 >= posInShip.y) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOutSight(Particle particle) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return true;
		}

		double x1 = particle.x, y1 = particle.y, z1 = particle.z, x2 = mc.player.getX(), y2 = mc.player.getY(), z2 = mc.player.getZ();

		var inWorldX1 = x1;
		var inWorldY1 = y1;
		var inWorldZ1 = z1;
		var inWorldX2 = x2;
		var inWorldY2 = y2;
		var inWorldZ2 = z2;

		var ship1 = VSGameUtilsKt.getShipManagingPos(particle.level, x1, y1, z1);
		if (ship1 != null) {
			Matrix4dc m = ship1.getShipToWorld();
			inWorldX1 = m.m00() * x1 + m.m10() * y1 + m.m20() * z1 + m.m30();
			inWorldY1 = m.m01() * x1 + m.m11() * y1 + m.m21() * z1 + m.m31();
			inWorldZ1 = m.m02() * x1 + m.m12() * y1 + m.m22() * z1 + m.m32();
		}

		var ship2 = VSGameUtilsKt.getShipManagingPos(particle.level, x2, y2, z2);
		if (ship2 != null) {
			Matrix4dc m = ship2.getShipToWorld();
			inWorldX2 = m.m00() * x2 + m.m10() * y2 + m.m20() * z2 + m.m30();
			inWorldY2 = m.m01() * x2 + m.m11() * y2 + m.m21() * z2 + m.m31();
			inWorldZ2 = m.m02() * x2 + m.m12() * y2 + m.m22() * z2 + m.m32();
		}

		int renderDistance = mc.levelRenderer.lastViewDistance << 4;

		return Math.abs(inWorldX2 - inWorldX1) > renderDistance
			   || Math.abs(inWorldY2 - inWorldY1) > renderDistance
			   || Math.abs(inWorldZ2 - inWorldZ1) > renderDistance;
	}
}
