package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.WeatherParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle implements WeatherParticleAddon {
	@Unique
	private boolean asyncparticles$invisible;
	@Unique
	private AABB asyncparticles$weathersAABB = INITIAL_AABB;

	@Shadow
	public abstract void remove();

	@Override
	public AABB asyncparticles$getWeatherAABB() {
		return asyncparticles$weathersAABB;
	}

	@Override
	public void asyncparticles$setWeatherAABB(AABB aabb) {
		asyncparticles$weathersAABB = aabb;
	}

	@Override
	public boolean asyncparticles$invisible() {
		return asyncparticles$invisible;
	}

	@Override
	public void asyncparticles$setInvisible(boolean visible) {
		asyncparticles$invisible = visible;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ParticleRainCompat.asyncparticles$particleCount.getAndIncrement();
		asyncparticles$setWeatherAABB(AABB.ofSize(new Vec3(x, y, z), 3.8, 3.8, 3.8));
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private void onRemove(CallbackInfo ci) {
		ParticleRainCompat.asyncparticles$particleCount.getAndDecrement();
	}

	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
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
		if (this.stoppedByCollision) {
			return;
		}
		double g = d;
		double h = e;
		double i = f;
		if (this.hasPhysics && (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) && d * d + e * e + f * f < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 position = new Vec3(x, y, z);
			Vec3 apply = Type.OTHER.collide(level, position, new Vec3(d, e, f), asyncparticles$getWeatherAABB());
			if (apply == null) {
				asyncparticles$setInvisible(true);
				remove();
				return;
			}
			Vec3 motion = Entity.collideBoundingBox(
				null,
				apply,
				getBoundingBox(),
				this.level,
				Collections.emptyList());
			d = motion.x;
			e = motion.y;
			f = motion.z;
		}

		if (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) {
			asyncparticles$setWeatherAABB(asyncparticles$getWeatherAABB().move(d, e, f));
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double) 1.0E-5F && Math.abs(e) < (double) 1.0E-5F) {
			this.stoppedByCollision = true;
		}

		this.onGround = h != e && h < (double) 0.0F;
		if (g != d) {
			this.xd = 0.0F;
		}

		if (i != f) {
			this.zd = 0.0F;
		}

	}

	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
		if (asyncparticles$invisible()) {
			return;
		}
		super.render(vertexConsumer, camera, f);
	}
}
