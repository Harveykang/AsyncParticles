package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerEntityShipCollisionUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import pigcart.particlerain.particle.WeatherParticle;

import java.util.List;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherPatricle extends TextureSheetParticle {
	@Unique
	protected boolean asyncparticles$invisible;

	@Unique
	protected AABB asyncparticles$weathersAABB;

	protected MixinWeatherPatricle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		asyncparticles$weathersAABB = AABB.ofSize(new Vec3(x, y, z), 3.8, 3.8, 3.8);
	}

	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.2))
	private double onTick(double original) {
		return 2.1;
	}

	/**
	 * @author
	 * @reason
	 */
	@Override
	public void move(double d, double e, double f) {
		if (!((InvokerEntityShipCollisionUtils) (Object) EntityShipCollisionUtils.INSTANCE)
			.invoker_getShipPolygonsCollidingWithEntity(null, new Vec3(d, e, f), asyncparticles$weathersAABB, level).isEmpty()) {
			asyncparticles$invisible = true;
			remove();
			return;
		}
		if (this.stoppedByCollision) {
			return;
		}
		double g = d;
		double h = e;
		double i = f;
		if (this.hasPhysics && (d != (double)0.0F || e != (double)0.0F || f != (double)0.0F) && d * d + e * e + f * f < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(d, e, f), this.getBoundingBox(), this.level, List.of());
			d = vec3.x;
			e = vec3.y;
			f = vec3.z;
		}

		if (d != (double)0.0F || e != (double)0.0F || f != (double)0.0F) {
			asyncparticles$weathersAABB = asyncparticles$weathersAABB.move(d, e, f);
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double)1.0E-5F && Math.abs(e) < (double)1.0E-5F) {
			this.stoppedByCollision = true;
		}

		this.onGround = h != e && h < (double)0.0F;
		if (g != d) {
			this.xd = 0.0F;
		}

		if (i != f) {
			this.zd = 0.0F;
		}

	}

	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
		if (asyncparticles$invisible) {
			return;
		}
		super.render(vertexConsumer, camera, f);
	}
}
