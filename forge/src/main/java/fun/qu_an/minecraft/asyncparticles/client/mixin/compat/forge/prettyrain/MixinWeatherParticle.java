package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.prettyrain;

import com.leclowndu93150.particlerain.particle.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollideUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.prettyrain.forge.PrettyRainAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.prettyrain.forge.PrettyRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
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

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle implements PrettyRainAddon {
	@Unique
	private boolean asyncparticles$invisible;
	@Unique
	private AABB asyncparticles$weathersAABB = INITIAL_AABB;

	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

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

	@SuppressWarnings("ConstantValue")
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		PrettyRainCompat.particleCount.getAndIncrement();
		if (StreakParticle.class.isInstance(this) ||
			RippleParticle.class.isInstance(this)) {
			asyncparticles$setWeatherAABB(AABB.ofSize(new Vec3(x, y, z), 0.01, 0.01, 0.01));
		} else {
			asyncparticles$setWeatherAABB(AABB.ofSize(new Vec3(x, y, z), 3.8, 3.8, 3.8));
		}
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private void onRemove(CallbackInfo ci) {
		PrettyRainCompat.particleCount.getAndDecrement();
	}

	@SuppressWarnings("ConstantValue")
	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.2))
	private double onTick(double original) {
		return ((Object) this instanceof SnowParticle) ||
			((Object) this instanceof RainParticle)
			? 2.1 : 0.2;
	}

	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
		if (asyncparticles$invisible()) {
			return;
		}
		super.render(vertexConsumer, camera, f);
	}

	@Override
	public void move(double d, double e, double f) {
		if (ModListHelper.CREATE_LOADED) {
			Vec3 mov = CollideUtil.collideMotionWithContraptions(level, new Vec3(d, e, f), getBoundingBox());
			if (mov != null) {
				d = mov.x;
				e = mov.y;
				f = mov.z;
				if ((Object) this instanceof RainParticle) {
					PrettyRainCompat.onCreateCollision(level, new Vec3(x, y, z), new Vec3(d, e, f), getBoundingBox());
				}
				asyncparticles$setInvisible(false);
				remove();
			}
		}
		if (ModListHelper.VS_LOADED) {
			Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(new Vec3(d, e, f), getBoundingBox(), level);
			if (shipMovement != null) {
				d = shipMovement.x;
				e = shipMovement.y;
				f = shipMovement.z;
				if ((Object) this instanceof RainParticle) {
					PrettyRainCompat.onShipCollision(level, new Vec3(x, y, z), shipMovement, getBoundingBox());
				}
				asyncparticles$setInvisible(false);
				remove();
			}
		}
		super.move(d, e, f);
	}
}
