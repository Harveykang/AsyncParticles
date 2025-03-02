package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.SnowParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SnowParticle.class)
public abstract class MixinSnowParticle extends MixinWeatherParticle {
	protected MixinSnowParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;below()Lnet/minecraft/core/BlockPos;"))
	private BlockPos onTick(BlockPos.MutableBlockPos instance) {
		return instance;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSize(3.8F, 3.8F);
	}

	@Override
	public void move(double d, double e, double f) {
		if (this.stoppedByCollision) {
			return;
		}
		double g = d;
		double h = e;
		double i = f;
		if (this.hasPhysics && (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) && d * d + e * e + f * f < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 originalMotion = new Vec3(d, e, f);
			Vec3 apply = WeatherParticleAddon.Type.SNOW.collide(level, new Vec3(x, y, z), originalMotion, asyncparticles$getWeatherAABB());
			if (apply == null) {
				asyncparticles$setInvisible(true);
				remove();
				return;
			}
			Vec3 motion = Entity.collideBoundingBox(
				null,
				apply,
				asyncparticles$getWeatherAABB(), // It looks good that way
				this.level,
				List.of());
			if (!apply.equals(originalMotion) && motion.equals(apply)) {
				d = apply.x;
				e = apply.y;
				f = apply.z;
			} else {
				double d1 = Math.abs(motion.y / h);
				double d2 = g * d1;
				d = Math.abs(d2) > Math.abs(motion.x) ? motion.x : d2;
				e = motion.y;
				double d3 = i * d1;
				f = Math.abs(d3) > Math.abs(motion.z) ? motion.z : d3;
			}
		}

		if (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) {
			asyncparticles$setWeatherAABB(asyncparticles$getWeatherAABB().move(d, e, f));
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double) 1.0E-5F && Math.abs(e) < (double) 1.0E-5F) {
			this.stoppedByCollision = true;
			asyncparticles$setInvisible(true);
		}

		if (h != e && h < (double) 0.0F) {
			this.onGround = true;
			asyncparticles$setInvisible(true);
		} else {
			this.onGround = false;
		}

		if (g != d) {
			this.xd = 0.0;
		}

		if (i != f) {
			this.zd = 0.0;
		}

	}
}
