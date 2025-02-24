package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;

import java.util.List;

import static java.lang.Math.*;

@Mixin(Particle.class)
public abstract class MixinParticle {
	@Shadow
	@Final
	public ClientLevel level;

	@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 collideBoundingBox(Entity entity, Vec3 vec3, AABB aABB, Level level, List<VoxelShape> list, Operation<Vec3> original) {
		// we do it in another thread, so we don't need to worry about costly collision checks
		double xsize = aABB.getXsize();
		double ysize = aABB.getYsize();
		double zsize = aABB.getZsize();
		return original.call(entity,
			EntityShipCollisionUtils.INSTANCE.adjustEntityMovementForShipCollisions(null,
				vec3,
				aABB.inflate(xsize >= 0.1 ? 0.0 : 0.1 - xsize, ysize >= 0.1 ? 0.0 : 0.1 - ysize, zsize >= 0.1 ? 0.0 : 0.1 - zsize),
				level),
			aABB, level, list);
	}
}
