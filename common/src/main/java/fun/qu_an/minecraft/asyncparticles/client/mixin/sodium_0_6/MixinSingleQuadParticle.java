package fun.qu_an.minecraft.asyncparticles.client.mixin.sodium_0_6;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.minecraft.client.particle.SingleQuadParticle;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SingleQuadParticle.class, priority = 1500)
public class MixinSingleQuadParticle {
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "renderRotatedQuad"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", remap = false,
		target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private static MemoryStack redirectStackPush() {
		return MemStackUtil.stackPush();
	}
}
