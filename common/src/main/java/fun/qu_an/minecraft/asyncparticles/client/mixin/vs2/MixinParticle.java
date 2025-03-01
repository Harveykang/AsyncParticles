package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
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

	/**
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.create.MixinParticle#collideBoundingBox}
	 */
	@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 collideBoundingBox(Entity entity, Vec3 vec3, AABB aABB, Level level, List<VoxelShape> list, Operation<Vec3> original) {
		// we do it in another thread, so we don't need to worry about costly collision checks
		double xsize = aABB.getXsize();
		double ysize = aABB.getYsize();
		double zsize = aABB.getZsize();
		Vec3 mov = VSClientUtils.entityMovColShipOnly(null,
			vec3,
			aABB.inflate(xsize >= 0.1 ? 0.0 : 0.1 - xsize, ysize >= 0.1 ? 0.0 : 0.1 - ysize, zsize >= 0.1 ? 0.0 : 0.1 - zsize),
			(ClientLevel) level);
		return original.call(entity,
			mov == null ? vec3 : mov,
			aABB, level, list);
	}
}
