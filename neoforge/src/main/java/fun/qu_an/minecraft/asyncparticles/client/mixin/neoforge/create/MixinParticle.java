package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateCompatImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Particle.class)
public abstract class MixinParticle {
	@Shadow
	@Final
	public ClientLevel level;

	@Shadow public double x;

	@Shadow public double y;

	@Shadow public double z;

	@Shadow public abstract AABB getBoundingBox();

	/**
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.MixinParticle#collideBoundingBox}
	 */
	@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 collideBoundingBox(Entity entity, Vec3 motion, AABB aABB, Level level, List<VoxelShape> list, Operation<Vec3> original) {
		// we do it in another thread, so we don't need to worry about costly collision checks
		Vec3 mov = CreateCompatImpl.collideMotionWithContraptions((ClientLevel) level, new Vec3(x, y, z), motion, getBoundingBox());
		return original.call(entity,
			mov == null ? motion : mov,
			aABB, level, list);
	}
}
