package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain_create;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollideUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollisionType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/TextureSheetParticle;tick()V", shift = At.Shift.AFTER))
	protected void onTick(CallbackInfo ci) {
		if (!isAlive()) {
			return;
		}
		Vec3 motion = new Vec3(xd, yd, zd);
		AABB aabb = getBoundingBox().inflate(quadSize * 0.35f);
		if (isAlive() && CollideUtil.isCollideWithContraptions(level, motion, aabb) != CollisionType.NONE) {
			remove();
		}
	}
}
