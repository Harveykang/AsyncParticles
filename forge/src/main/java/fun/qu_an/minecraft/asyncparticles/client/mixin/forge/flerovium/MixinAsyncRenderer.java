package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.flerovium;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AsyncRenderer.class)
public class MixinAsyncRenderer {
	@Redirect(method = "renderParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
	private static boolean isVisible(Frustum frustum, AABB aabb, @Local(argsOnly = true) Particle particle) {
		return flerovium$FastFrustumCheck(frustum, aabb, particle);
	}

	/**
	 * &#064;See  {@link com.moepus.flerovium.mixins.Particle.ParticleEngineMixin#FastFrustumCheck)}<p>
	 * **Note: this method is under LGPL license, author: Moepus**<p>
	 * This may be a violation of LGPL...<p>
	 * but mixin classes cannot reference directly :(
	 */
	@Unique
	private static boolean flerovium$FastFrustumCheck(Frustum instance, AABB aabb, Particle particle) {
		if (aabb.minX == Double.NEGATIVE_INFINITY) {
			return true;
		} else {
			float x = (float) (particle.x - instance.camX);
			float y = (float) (particle.y - instance.camY);
			float z = (float) (particle.z - instance.camZ);
			float width = particle.bbWidth;
			float height = particle.bbHeight;
			return instance.intersection.testSphere(x, y, z, Math.max(width, height) * 0.5F);
		}
	}
}
