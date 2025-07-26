package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ModConfig;
import pigcart.particlerain.particle.CustomParticle;

@Mixin(CustomParticle.class)
public abstract class MixinCustomParticle extends MixinWeatherParticle implements ParticleRainAddon {
	@Shadow(remap = false)
	ModConfig.ParticleOptions opts;

	protected MixinCustomParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "testForCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
	protected BlockHitResult redirectClip(ClientLevel level,
										  ClipContext clipContext,
										  @Local(ordinal = 0) Vec3 quadCenterPos,
										  @Local(ordinal = 1) Vec3 quadEdgePos) {
		if (!asyncparticles$isRainParticle()) {
			return level.clip(clipContext);
		}
		BlockHitResult clip = VSClientUtils.clipVanillaAndShip(level, clipContext, true);
		if (clip.getType() == HitResult.Type.BLOCK && !clip.isInside() && clip instanceof ShipHitResult result) {
			ParticleRainCompat.onShipCollision(level, result.location, result.shipMotion);
		}
		return clip;
	}

	@Override
	protected void onTick(CallbackInfo ci) {
		if (VSClientUtils.isEntityMovColShipOnly(new Vec3(xd, yd, zd), getBoundingBox(), level, opts.size * 0.5f)) {
			remove();
		}
	}
}
