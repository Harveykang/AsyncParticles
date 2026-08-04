package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particletweaks;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particletweaks.ParticleTweaksCompat;
import net.minecraft.client.particle.SingleQuadParticle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SingleQuadParticle.class, priority = 1500)
public abstract class MixinMixinSingleQuadParticle implements GpuParticleAddon {
	@Unique
	private static boolean asyncparticles$compatBroken = false;

	@TargetHandler(
		name = "asyncparticles$getLayer",
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration.MixinSingleQuadParticle"
	)
	@ModifyReturnValue(method = "@MixinSquared:Handler", at = @At("RETURN"))
	public SingleQuadParticle.Layer wrapGetLayer(SingleQuadParticle.Layer original) {
		if (asyncparticles$compatBroken) {
			return original;
		}
		try {
			return ParticleTweaksCompat.modifyLayer(this, original);
		} catch (Throwable t) {
			asyncparticles$compatBroken = true;
			return original;
		}
	}

	@TargetHandler(
		name = "asyncparticles$getQuadSize",
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration.MixinSingleQuadParticle"
	)
	@WrapMethod(method = "@MixinSquared:Handler")
	public float wrapGetQuadSize(float partialTickTime, Operation<Float> original) {
		float call = original.call(partialTickTime);
		if (asyncparticles$compatBroken) {
			return call;
		}
		try {
			return ParticleTweaksCompat.modifyQuadSize(this, partialTickTime, call);
		} catch (Throwable t) {
			asyncparticles$compatBroken = true;
			return call;
		}
	}

	@Dynamic
	@TargetHandler(
		name = "asyncparticles$getOColor",
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration.MixinSingleQuadParticle"
	)
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/client/particle/SingleQuadParticle;alpha:F"))
	public float wrapGetOColor(float original) {
		if (asyncparticles$compatBroken) {
			return original;
		}
		try {
			return ParticleTweaksCompat.modifyOColor(this, original);
		} catch (Throwable t) {
			asyncparticles$compatBroken = true;
			return original;
		}
	}

	@TargetHandler(
		name = "asyncparticles$getColor",
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration.MixinSingleQuadParticle"
	)
	@ModifyReturnValue(method = "@MixinSquared:Handler", at = @At("RETURN"))
	public int wrapGetColor(int original) {
		if (asyncparticles$compatBroken) {
			return original;
		}
		try {
			return ParticleTweaksCompat.modifyColor(this, original);
		} catch (Throwable t) {
			asyncparticles$compatBroken = true;
			return original;
		}
	}
}
