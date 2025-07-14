package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@WrapOperation(method = "testForCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
	private BlockHitResult a(ClientLevel instance,
							 ClipContext clipContext,
							 Operation<BlockHitResult> original,
							 @Local(ordinal = 0) Vec3 quadCenterPos,
							 @Local(ordinal = 1) Vec3 quadEdgePos){
		BlockHitResult clip = CreateUtil.clip(level, quadCenterPos, quadEdgePos);
		return clip != null ? clip : original.call(instance, clipContext);
	}

//	@Override
//	public void move(double x, double y, double z) {
//		if (!this.stoppedByCollision) {
//			double d = x;
//			double e = y;
//			double f = z;
//
//			float size = Math.max(Math.max(bbHeight, bbWidth), quadSize) * 0.5f;
//			AABB aabb = new AABB(this.x - size, this.y - size, this.z - size, this.x + size, this.y + size, this.z + size);
//			Vec3 vec3 = CreateUtil.collideMotionWithContraptions(this.level, new Vec3(x, y, z), aabb);
//
//			if (vec3 != null) {
//				x = vec3.x;
//				y = vec3.y;
//				z = vec3.z;
//			}
//
//			if (x != 0.0d || y != 0.0d || z != 0.0d) {
//				this.setBoundingBox(this.getBoundingBox().move(x, y, z));
//				this.setLocationFromBoundingbox();
//			}
//
//			if (Math.abs(e) >= 1.0E-5d && Math.abs(y) < 1.0E-5d) {
//				this.stoppedByCollision = true;
//			}
//
//			this.onGround = e != y && e < 0.0d;
//			if (d != x) {
//				this.xd = 0.0d;
//			}
//
//			if (f != z) {
//				this.zd = 0.0d;
//			}
//
//		}
//	}
}
