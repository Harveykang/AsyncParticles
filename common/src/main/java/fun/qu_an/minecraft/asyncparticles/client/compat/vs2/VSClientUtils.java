package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerEntityShipCollisionUtils;
import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerRaycastUtils;
import kotlin.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.collision.ConvexPolygonc;
import org.valkyrienskies.core.apigame.collision.EntityPolygonCollider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.util.BugFixUtil;

import java.util.List;

import static java.lang.Math.abs;
import static net.minecraft.util.Mth.floor;
import static org.valkyrienskies.core.util.AABBdUtilKt.extend;
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
	public static boolean isEntityMovColShipOnly(
		@Nullable Entity entity,
		Vec3 movement,
		AABB entityBoundingBox,
		ClientLevel world,
		double inflation) {
		double stepHeight = (entity != null) ? entity.maxUpStep() : 0.0;
		double yMovement = movement.y() + Math.max(stepHeight - inflation, 0.0);
		Vec3 movement1 = new Vec3(movement.x(), yMovement, movement.z());
		AABB bb = entityBoundingBox.inflate(inflation);
		return hasShipPolygonsCollidingWithEntity(entity, movement1, bb, world);
	}

	/**
	 * @see EntityShipCollisionUtils#getShipPolygonsCollidingWithEntity
	 */
	public static boolean hasShipPolygonsCollidingWithEntity(Entity entity,
															 Vec3 movement,
															 AABB entityBoundingBox,
															 ClientLevel world) {
		AABB entityBoxWithMovement = entityBoundingBox.expandTowards(movement);
		AABBdc entityBoundingBoxExtended = extend(toJOML(entityBoundingBox), toJOML(movement));

		for (ClientShip shipObject : VSGameUtilsKt.getShipObjectWorld(world).getLoadedShips().getIntersecting(entityBoundingBoxExtended)) {
			ShipTransform shipTransform = shipObject.getTransform();
			ConvexPolygonc entityPolyInShipCoordinates = collider.createPolygonFromAABB(
				toJOML(entityBoxWithMovement),
				shipTransform.getWorldToShip(),
				null
			);
			AABBdc entityBoundingBoxInShipCoordinates = entityPolyInShipCoordinates.getEnclosingAABB(new AABBd());
			if (BugFixUtil.INSTANCE.isCollisionBoxTooBig(toMinecraft(entityBoundingBoxInShipCoordinates))) {
				// Box too large, skip it
				continue;
			}

			Iterable<VoxelShape> shipBlockCollisionStream = world.getBlockCollisions(entity, toMinecraft(entityBoundingBoxInShipCoordinates));
			if (shipBlockCollisionStream.iterator().hasNext()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * No vanilla collision check.
	 */
	public static boolean isEntityMovColShipOnly(
		@Nullable Entity entity,
		Vec3 movement,
		AABB entityBoundingBox,
		ClientLevel world) {
		double inflation = (entity instanceof Player) ? 0.5 : 0.1;
		return isEntityMovColShipOnly(entity, movement, entityBoundingBox, world, inflation);
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
		double inflation,
		double stepHeight) {
		// Inflate the bounding box more for players than other entities, to give players a better collision result.
		// Note that this increases the cost of doing collision, so we only do it for the players
		if (entity != null) {
			stepHeight = entity.maxUpStep();
		}
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

		if (shipCollidingWith == null) {
			return null;
		}

		if (entity != null) {
			((IEntityDraggingInformationProvider) entity).getDraggingInformation().setLastShipStoodOn(shipCollidingWith);
			return toMinecraft(newMovement);
		} else {
			ClientShip ship = VSGameUtilsKt.getShipObjectWorld(world).getLoadedShips().getById(shipCollidingWith);
			if (ship == null) {
				return null;
			}
			Vector3dc velocity = ship.getVelocity();
			return new Vec3(0.05 * velocity.x() + newMovement.x(),
				0.05 * velocity.y() + newMovement.y(),
				0.05 * velocity.z() + newMovement.z());
		}
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
		return entityMovColShipOnly(entity, movement, entityBoundingBox, world, inflation, 0.0);
	}

	public static Vec3 entityMovColShipOnly(Vec3 movement, AABB entityBoundingBox, ClientLevel world, double inflation, double stepHeight) {
		return entityMovColShipOnly(null, movement, entityBoundingBox, world, inflation, stepHeight);
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
			Matrix4dc worldToShip = ship.getWorldToShip();
			Matrix4dc shipToWorld = ship.getShipToWorld();
			var shipStart = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getFrom())));
			var shipEnd = toMinecraft(worldToShip.transformPosition(toJOML(ctx.getTo())));

			var shipHit = InvokerRaycastUtils.invoker_clip(level, ctx, shipStart, shipEnd);
			Vector3d worldPos = shipToWorld.transformPosition(toJOML(shipHit.getLocation()));
			var shipHitPos = toMinecraft(worldPos);

			var shipHitDist = shipHitPos.distanceToSqr(ctx.getFrom());

			if (shipHitDist < closestHitDist && shipHit.getType() != HitResult.Type.MISS) {
				var newPosInShipLocal = worldPos
					.sub(ship.getTransform().getPositionInWorld());
				var shipVelocity = new Vector3d(ship.getVelocity())
					.add(new Vector3d(ship.getOmega()).cross(newPosInShipLocal))
					.mul(0.05);
				closestHit = ShipHitResult.of(shipHit, worldToShip, shipToWorld,
					new Vec3(shipVelocity.x(), shipVelocity.y(), shipVelocity.z()));
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

	public static boolean isUnderHeightMapIncludeShips(ClientLevel level, double x, double y, double z, int size) {
		if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor(x), floor(z)) >= y) {
			return true;
		}
		return isUnderShipHeightMap(level, x, y, z, size);
	}

	public static boolean isUnderShipHeightMap(ClientLevel level, double x, double y, double z, double size) {
		var shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
		for (var nearbyShip : shipObjectWorld.getLoadedShips().getIntersecting(
			new AABBd(x - 1, y - 1, z - 1, x + 1, Math.max(y + 16, level.getMaxBuildHeight()), z + 1))) {
			var posInShip = nearbyShip.getWorldToShip().transformPosition(new Vector3d(x, y, z));
			if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor(posInShip.x), floor(posInShip.z)) >= posInShip.y - size) {
				return true;
			}
		}
		return false;
	}

	public static boolean isUnderShipHeightMap(ClientLevel level, Vec3 pos, int size) {
		return isUnderShipHeightMap(level, pos.x, pos.y, pos.z, size);
	}

	public static boolean isUnderShipHeightMap(ClientLevel level, Vec3 pos, Matrix4dc worldToShip) {
		var posInShip = worldToShip.transformPosition(toJOML(pos));
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, floor(posInShip.x), floor(posInShip.z)) >= posInShip.y;
	}

	public static boolean isOutOfSight(ClientLevel level, double x1, double y1, double z1) {
		double inWorldX1 = x1;
		double inWorldZ1 = z1;

		var ship1 = VSGameUtilsKt.getShipManagingPos(level, x1, y1, z1);
		if (ship1 != null) {
			Matrix4dc m = ship1.getShipToWorld();
			inWorldX1 = m.m00() * x1 + m.m10() * y1 + m.m20() * z1 + m.m30();
			inWorldZ1 = m.m02() * x1 + m.m12() * y1 + m.m22() * z1 + m.m32();
		}

		Minecraft mc = Minecraft.getInstance();
		int renderDistance = mc.options.getEffectiveRenderDistance() << 4;
		Camera camera = mc.gameRenderer.getMainCamera();

		Vec3 pos = camera.getPosition();
		return abs(pos.x - inWorldX1) > renderDistance
			   || abs(pos.z - inWorldZ1) > renderDistance;
	}
}
