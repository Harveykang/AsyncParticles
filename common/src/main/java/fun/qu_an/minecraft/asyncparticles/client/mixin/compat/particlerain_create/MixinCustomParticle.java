package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain_create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollideUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ParticleData;
import pigcart.particlerain.particle.CustomParticle;

@Mixin(CustomParticle.class)
public abstract class MixinCustomParticle extends TextureSheetParticle {
	@Unique
	private boolean asyncparticles$isRain;

	protected MixinCustomParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(ClientLevel level, double x, double y, double z, ParticleData opts, CallbackInfo ci){
		asyncparticles$isRain = opts.id.contains("rain") && opts.needsSkyAccess;
	}

	@WrapOperation(method = "tickCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
	protected BlockHitResult wrapClip(ClientLevel level,
	                                  ClipContext clipContext,
	                                  Operation<BlockHitResult> original,
	                                  @Local(name = "quadCenterPos") Vec3 quadCenterPos,
	                                  @Local(name = "quadEdgePos") Vec3 quadEdgePos) {
		if (!asyncparticles$isRain || isAlive()) {
			return original.call(level, clipContext);
		}
		ContraptionHitResult hitResult = CollideUtil.rayCastWithContactPointMotion(level, quadCenterPos, quadEdgePos);
		if (hitResult == null) {
			return original.call(level, clipContext);
		}
		if (hitResult.getType() == HitResult.Type.BLOCK && !hitResult.isInside()) {
			ParticleRainCompat.onCreateCollision(level, hitResult.location, hitResult.contactPointMotion);
		}
		return hitResult;
	}
}
