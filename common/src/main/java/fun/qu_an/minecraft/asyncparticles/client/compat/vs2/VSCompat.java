package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.Matrix3d;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class VSCompat {
	public static final ParticleThreadLocal<Integer> shipSurfaceY = ParticleThreadLocal.withInitial(() -> Integer.MIN_VALUE);

	public static boolean canSpawnWeatherParticle(ClientLevel level, double x, double y, double z) {
		return !VSClientUtils.isUnderShipHeightMap(level, x, y, z, 0.5);
	}

	public static boolean canSpawnWeatherParticle(ClientLevel level, double x, double y, double z, double size) {
		return !VSClientUtils.isUnderShipHeightMap(level, x, y, z, size);
	}

	public static Vec3 getContactPointMotion(Vec3 original,
	                                         AbstractContraptionEntity entity,
	                                         Vec3 contactPoint) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(entity.level(), entity.getAnchorVec());
		if (ship == null) {
			return original;
		}
		Matrix4dc worldToShip = ship.getWorldToShip();
		Vector3d localContactPoint = worldToShip.transformPosition(contactPoint.x, contactPoint.y, contactPoint.z, new Vector3d());
		Vector3d contactPointMotion = ValkyrienSkies.toJOML(entity.getContactPointMotion(ValkyrienSkies.toMinecraft(localContactPoint)));
		Vector3d vector3d/* localContactPoint */ = ship.getPrevTickTransform().getShipToWorld().transformPosition(localContactPoint);
		Vector3d result = worldToShip.transformDirection(contactPointMotion)
			/* contactPointMotion */.add(vector3d/* localContactPoint */.sub(contactPoint.x, contactPoint.y, contactPoint.z).negate());
		return ValkyrienSkies.toMinecraft(result);
	}

	public static AABB getBoundingBox(AABB original,
	                                  AbstractContraptionEntity entity) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(entity.level(), entity.getAnchorVec());
		if (ship == null) {
			return original;
		}
		AABBd aabBd = ValkyrienSkies.toJOML(original);
		aabBd.transform(ship.getShipToWorld());
		return ValkyrienSkies.toMinecraft(aabBd);
	}

	public static AABB expandTowards(AABB original,
	                                 AbstractContraptionEntity entity) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(entity.level(), entity.getAnchorVec());
		if (ship == null) {
			return original;
		}
		AABBd aabBd = ValkyrienSkies.toJOML(original.inflate(2.0));
		aabBd.transform(ship.getShipToWorld());
		return ValkyrienSkies.toMinecraft(aabBd);
	}

	public static Vec3 getAnchorVec(Vec3 original,
	                                AbstractContraptionEntity entity) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(entity.level(), entity.getAnchorVec());
		if (ship == null) {
			return original;
		}
		Vector3d result = ship.getShipToWorld().transformPosition(ValkyrienSkies.toJOML(original).add(0.5, 0.5, 0.5)).sub(0.5, 0.5, 0.5);
		return ValkyrienSkies.toMinecraft(result);
	}

	public static Matrix3d asMatrix(Matrix3d original,
	                                AbstractContraptionEntity entity) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(entity.level(), entity.getAnchorVec());
		if (ship == null) {
			return original;
		}
		org.joml.Matrix3d jomlMatrix = CreateUtil.toJOML(original);
		jomlMatrix.mulLocal(ship.getShipToWorld().get3x3(new org.joml.Matrix3d()));
		return CreateUtil.toCreate(jomlMatrix);
	}
}
