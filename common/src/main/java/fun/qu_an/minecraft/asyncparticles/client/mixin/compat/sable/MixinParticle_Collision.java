package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.Particle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinParticle_Collision {
	@Unique
	private static final ParticleThreadLocal<BoundingBox3d> asyncparticles$collisionBox =
		ParticleThreadLocal.withInitial(ThreadUtil::isOnMainThread, BoundingBox3d::new);

	@TargetHandler(mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin", name = "resolveAABBCollision")
	@ModifyExpressionValue(method = "@MixinSquared:Handler", remap = false,
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/Particle;TEMP_BOX:Ldev/ryanhcode/sable/companion/math/BoundingBox3d;", opcode = Opcodes.GETSTATIC))
	private static BoundingBox3d threadLocalCollisionBox(BoundingBox3d original) {
		return asyncparticles$collisionBox.get();
	}

	@TargetHandler(mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin", name = "lambda$resolveAABBCollision$0")
	@ModifyExpressionValue(method = "@MixinSquared:Handler", remap = false,
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/Particle;TEMP_BOX:Ldev/ryanhcode/sable/companion/math/BoundingBox3d;", opcode = Opcodes.GETSTATIC))
	private static BoundingBox3d threadLocalShapeCollisionBox(BoundingBox3d original) {
		return asyncparticles$collisionBox.get();
	}
}
