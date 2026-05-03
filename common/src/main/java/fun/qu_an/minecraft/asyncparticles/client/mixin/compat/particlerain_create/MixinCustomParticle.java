//package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_create;
//
//import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//import com.llamalad7.mixinextras.sugar.Local;
//import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.ContraptionHitResult;
//import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtil;
//import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainAddon;
//import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainCompat;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.world.level.ClipContext;
//import net.minecraft.world.phys.BlockHitResult;
//import net.minecraft.world.phys.HitResult;
//import net.minecraft.world.phys.Vec3;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import pigcart.particlerain.particle.CustomParticle;
//
//@Mixin(CustomParticle.class)
//public abstract class MixinCustomParticle implements ParticleRainAddon {
//	@WrapOperation(method = "testForCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
//	protected BlockHitResult wrapClip(ClientLevel level,
//									  ClipContext clipContext,
//									  Operation<BlockHitResult> original,
//									  @Local(ordinal = 0) Vec3 quadCenterPos,
//									  @Local(ordinal = 1) Vec3 quadEdgePos) {
//		if (!asyncparticles$isNeedSkyAccess()) {
//			return original.call(level, clipContext);
//		}
//		ContraptionHitResult clip = CreateUtil.clipWithContactPointMotion(level, quadCenterPos, quadEdgePos);
//		if (clip == null) {
//			return original.call(level, clipContext);
//		}
//		if (clip.getType() == HitResult.Type.BLOCK && !clip.isInside()) {
//			ParticleRainCompat.onCreateCollision(level, clip.location, clip.contactPointMotion);
//		}
//		return clip;
//	}
//}
