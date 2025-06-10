package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;

import java.util.List;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinParticle implements LightCachedParticleAddon, VSParticleAddon {
	@Shadow
	@Final
	public ClientLevel level;
	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Unique
	protected ClientShip asyncparticles$vsShip;

	/**
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.create.MixinParticle#collideBoundingBox}
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.forge.weather2_vs.MixinEntityRotFX#collideBoundingBox}
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

	@TargetHandler(
		name = "checkShipCoords",
		// FIXME: This is unstable
		mixin = "org.valkyrienskies.mod.mixin.feature.transform_particles.MixinParticle"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("TAIL"))
	private void checkShipCoords(CallbackInfo ci,
								 @SuppressWarnings("LocalMayBeArgsOnly")
								 @Local(ordinal = 0)
								 ClientShip ship) {
		if (!asyncparticles$isOnShip()) {
			asyncparticles$setShip(ship);
			asyncparticles$refresh();
		}
	}

	@Override // inject after MixinParticle_LightCache to override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = BlockPos.containing(x, y, z);
		int light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		if (asyncparticles$vsShip == null || !ConfigHelper.fixParticleLightOnVsShips()) {
			asyncparticles$setLight(light);
		} else {
			Vector3d transformed = asyncparticles$vsShip.getWorldToShip().transformPosition(x, y, z, new Vector3d());
			BlockPos pos = BlockPos.containing(transformed.x, transformed.y, transformed.z);
			int shipLight = level.hasChunkAt(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
			int finalLight = Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
							 Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
			asyncparticles$setLight(finalLight);
		}
	}

	@Override
	public void asyncparticles$setShip(ClientShip ship) {
		asyncparticles$vsShip = ship;
	}

	@Override
	public boolean asyncparticles$isOnShip() {
		return asyncparticles$vsShip != null;
	}
}
