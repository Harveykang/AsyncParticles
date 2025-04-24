package fun.qu_an.minecraft.asyncparticles.client.mixin.sodium_0_7;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// To be compatible with async buffer filling
// Just trust the JIT
@Mixin(value = SingleQuadParticle.class, priority = 1500)
public class MixinSingleQuadParticle {
	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "render"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/SingleQuadParticle;TEMP_LEFT:Lorg/joml/Vector3f;"))
	private static Vector3f redirectTempLeft(Vector3f original) {
		return new Vector3f();
	}
	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "renderRotatedQuad"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/SingleQuadParticle;TEMP_LEFT:Lorg/joml/Vector3f;"))
	private static Vector3f redirectTempLeft2(Vector3f original) {
		return new Vector3f();
	}

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "render"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/SingleQuadParticle;TEMP_UP:Lorg/joml/Vector3f;"))
	private static Vector3f redirectTempUp(Vector3f original) {
		return new Vector3f();
	}

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "renderRotatedQuad"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/SingleQuadParticle;TEMP_UP:Lorg/joml/Vector3f;"))
	private static Vector3f redirectTempUp2(Vector3f original) {
		return new Vector3f();
	}
}
