package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.sable_create;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.Matrix3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollisionType;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CollideUtilImpl;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create.AccessorMatrix3d;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// code from https://github.com/ryanhcode/sable/blob/main/neoforge/src/main/java/dev/ryanhcode/sable/neoforge/mixin/compatibility/create/contraptions/ContraptionColliderMixin.java
// License: https://github.com/ryanhcode/sable/blob/c17017d1030a7ebc862cb0fc12d1f9bf3f1fe6a2/LICENSE.md
@Mixin(value = CollideUtilImpl.class, remap = false)
public class MixinCollideUtil {
	@Unique
	private static org.joml.Matrix3d asyncparticles$toJOML(Matrix3d createMatrix) {
		org.joml.Matrix3d jomlMatrix = new org.joml.Matrix3d();

		AccessorMatrix3d accessor = ((AccessorMatrix3d) createMatrix);
		jomlMatrix.set(
			accessor.m00(), accessor.m01(), accessor.m02(),
			accessor.m10(), accessor.m11(), accessor.m12(),
			accessor.m20(), accessor.m21(), accessor.m22()
		);

		return jomlMatrix;
	}

	@Unique
	private static Matrix3d asyncparticles$toCreate(org.joml.Matrix3d jomlMatrix) {
		Matrix3d createMatrix = new Matrix3d();
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

	@Inject(method = {
					"isCollideWithContraption",
					 "collideMotionWithContraption"
	}, at = @At("HEAD"))
	private static void asyncparticles$head(CallbackInfoReturnable<CollisionType> cir,
	                                        @Local(argsOnly = true) AbstractContraptionEntity instance,
	                                        @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = Sable.HELPER.getContaining(instance);
		contraptionSubLevel.set(subLevel);
	}

	@Redirect(method = {
		"isCollideWithContraption",
		"collideMotionWithContraption"
	}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
	private static AABB asyncparticles$contraptionBounds(AbstractContraptionEntity instance, @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = contraptionSubLevel.get();

		if (subLevel != null) {
			BoundingBox3d globalBB = new BoundingBox3d(instance.getBoundingBox());
			globalBB.transform(subLevel.logicalPose(), globalBB);
			return globalBB.toMojang();
		}

		return instance.getBoundingBox();
	}

	@Redirect(method = {
		"isCollideWithContraption",
		"collideMotionWithContraption"
	}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"))
	private static AABB asyncparticles$entityQueryBounds(AABB instance, Vec3 vec3, @Local(argsOnly = true) AbstractContraptionEntity contraption, @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = contraptionSubLevel.get();

		if (subLevel != null) {
			BoundingBox3d globalBB = new BoundingBox3d(contraption.getBoundingBox().inflate(2.0).expandTowards(vec3));
			globalBB.transform(subLevel.logicalPose(), globalBB);
			return globalBB.toMojang();
		}

		return instance.expandTowards(vec3);
	}

	@Redirect(method = {
		"isCollideWithContraption",
		"collideMotionWithContraption"
	}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;getAnchorVec()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 asyncparticles$getAnchorVec(AbstractContraptionEntity instance, @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = contraptionSubLevel.get();

		if (subLevel != null) {
			return subLevel.logicalPose().transformPosition(instance.getAnchorVec().add(0.5, 0.5, 0.5)).subtract(0.5, 0.5, 0.5);
		}

		return instance.getAnchorVec();
	}

	@Redirect(method = {
		"isCollideWithContraption",
		"collideMotionWithContraption"
	}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity$ContraptionRotationState;asMatrix()Lcom/simibubi/create/foundation/collision/Matrix3d;"))
	private static Matrix3d asyncparticles$rotationMatrix(AbstractContraptionEntity.ContraptionRotationState rotationState, @Local(argsOnly = true) AbstractContraptionEntity contraption, @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = contraptionSubLevel.get();
		if (subLevel != null) {
			Pose3d pose = subLevel.logicalPose();
			org.joml.Matrix3d jomlMatrix = asyncparticles$toJOML(rotationState.asMatrix());

			jomlMatrix.rotateLocal(pose.orientation());
			return asyncparticles$toCreate(jomlMatrix);
		}

		return rotationState.asMatrix();
	}

	@Redirect(method = {
		"isCollideWithContraption",
		"collideMotionWithContraption"
	}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;getContactPointMotion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 asyncparticles$getContactPointMotion(AbstractContraptionEntity instance, Vec3 globalContactPoint, @Share("subLevel") LocalRef<SubLevel> contraptionSubLevel) {
		SubLevel subLevel = contraptionSubLevel.get();
		if (subLevel != null) {
			Pose3d pose = subLevel.logicalPose();
			Vec3 localContactPoint = pose.transformPositionInverse(globalContactPoint);
			return pose.transformNormal(instance.getContactPointMotion(localContactPoint))
				.add(globalContactPoint.subtract(subLevel.lastPose().transformPosition(localContactPoint)));
		}

		return instance.getContactPointMotion(globalContactPoint);
	}
}
