package fun.qu_an.minecraft.asyncparticles.client.mixin.forge_particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import com.leclowndu93150.particlerain.particle.SnowParticle;

import java.util.List;

@Mixin(value = SnowParticle.class, remap = false)
public abstract class MixinSnowParticle extends MixinWeatherPatricle {
	protected MixinSnowParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
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
			Vec3 vec3 = new Vec3(d, e, f);
			Vec3 shipMovement = VSClientUtils.adjustEntityMovementForShipCollisions(null, vec3, weathersAABB, level);
			if (shipMovement == null) {
				vec3 = Entity.collideBoundingBox(null, vec3, getBoundingBox(), this.level, List.of());
			} else {
				vec3 = shipMovement;
			}
			d = vec3.x;
			e = vec3.y;
			f = vec3.z;
		}

		if (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) {
			weathersAABB = weathersAABB.move(d, e, f);
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double) 1.0E-5F && Math.abs(e) < (double) 1.0E-5F) {
			this.stoppedByCollision = true;
			invisible = true;
		}

		if (h != e && h < (double) 0.0F) {
			this.onGround = true;
			invisible = true;
		}

		if (g != d) {
			this.xd = 0.0;
		}

		if (i != f) {
			this.zd = 0.0;
		}

	}
}
