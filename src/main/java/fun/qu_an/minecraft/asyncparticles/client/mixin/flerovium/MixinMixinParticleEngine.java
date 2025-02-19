package fun.qu_an.minecraft.asyncparticles.client.mixin.flerovium;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public class MixinMixinParticleEngine {
	@SuppressWarnings("UnresolvedMixinReference")
	@TargetHandler(
		name = "lambda$render$0",
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticleEngine"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
	private static boolean isVisible(Frustum frustum, AABB aabb, @Local(argsOnly = true) Particle particle) {
		return asyncparticles$FastFrustumCheck$from_flerovium(frustum, aabb, particle);
	}

	/**
	 * &#064;See  {@link com.moepus.flerovium.mixins.Particle.ParticleEngineMixin#FastFrustumCheck)}
	 */
	@Unique
	private static boolean asyncparticles$FastFrustumCheck$from_flerovium(Frustum instance, AABB aabb, Particle particle) {
		if (aabb.minX == Double.NEGATIVE_INFINITY) {
			return true;
		} else {
			float x = (float)(particle.x - instance.camX);
			float y = (float)(particle.y - instance.camY);
			float z = (float)(particle.z - instance.camZ);
			float width = particle.bbWidth;
			float height = particle.bbHeight;
			return instance.intersection.testSphere(x, y, z, Math.max(width, height) * 0.5F);
		}
	}
}
