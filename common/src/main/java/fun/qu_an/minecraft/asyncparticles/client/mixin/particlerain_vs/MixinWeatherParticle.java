package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Redirect(method = "testForCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
	private BlockHitResult a(ClientLevel instance,
							 ClipContext clipContext,
							 @Local(ordinal = 0) Vec3 quadCenterPos,
							 @Local(ordinal = 1) Vec3 quadEdgePos){
		BlockHitResult clip = VSClientUtils.clipIncludeShips(level, clipContext, true);
		if (clip.getType() == HitResult.Type.BLOCK && !clip.isInside() && clip instanceof ShipHitResult result){
			ParticleRainCompat.onShipCollision(level, result.location, result.shipMotion);
		}
		return clip;
	}
}
