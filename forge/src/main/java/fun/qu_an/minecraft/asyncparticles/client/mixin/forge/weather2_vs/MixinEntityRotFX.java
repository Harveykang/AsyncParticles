package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.weather2_vs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.ParticleTexExtraRender;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRotFX.class)
public abstract class MixinEntityRotFX extends TextureSheetParticle {
	@Shadow
	public abstract void remove();

	protected MixinEntityRotFX(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "spawnAsWeatherEffect", remap = false, at = @At(value = "HEAD"), cancellable = true)
	private void spawnAsWeatherEffect(CallbackInfo ci) {
		if (!VSCompat.canCreateWeatherParticle(level, x, y, z)) {
			remove();
			ci.cancel();
		}
	}

	/**
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.create.MixinParticle#collideBoundingBox}
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.MixinParticle#collideBoundingBox}
	 */
	@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 collideBoundingBox(Entity entity, Vec3 vec3, AABB aabb, Level level, List<VoxelShape> list, Operation<Vec3> original) {
		if ((Object) this instanceof ParticleTexExtraRender) {
			return original.call(entity, vec3, aabb, level, list);
		}
		// we do it in the other thread, so we don't need to worry about costly collision checks
		double xsize = aabb.getXsize();
		double ysize = aabb.getYsize();
		double zsize = aabb.getZsize();

		AABB aabb1;
		if (xsize < 0.1 || ysize < 0.1 || zsize < 0.1){
			aabb1 = aabb.inflate(xsize >= 0.1 ? 0.0 : 0.1 - xsize, ysize >= 0.1 ? 0.0 : 0.1 - ysize, zsize >= 0.1 ? 0.0 : 0.1 - zsize);
		} else {
			aabb1 = aabb;
		}
		Vec3 mov = VSClientUtils.entityMovColShipOnly(null,
			vec3,
			aabb1,
			(ClientLevel) level);
		return original.call(entity,
			mov == null ? vec3 : mov,
			aabb, level, list);
	}
}
