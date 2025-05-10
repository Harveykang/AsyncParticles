package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.porting_lib_base;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 550)
public class MixinMixinParticleEngine {
	@Shadow public static List<ParticleRenderType> RENDER_ORDER;

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.tick.MixinParticleEngine",
		name = "lambda$tick$6"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("RETURN"))
	private void port_lib$addCustomRenderTypes(boolean tickAsync,
											   ParticleRenderType particleRenderType,
											   CallbackInfoReturnable<Queue<?>> cir) {
		if (!RENDER_ORDER.contains(particleRenderType)) {
			port_lib$addRenderType(particleRenderType);
		}
	}

	// priority = 550 so this will not override PortingLib's implementation
	@SuppressWarnings("MissingUnique")
	private static void port_lib$addRenderType(ParticleRenderType particleRenderType) {
	}
}
