package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.porting_lib_base;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 550)
public class MixinMixinParticleEngine {
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.tick.MixinParticleEngine",
		name = "lambda$tick$6"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("RETURN"))
	private void addCustomRenderTypes(boolean tickAsync,
									  ParticleRenderType particleRenderType,
									  CallbackInfoReturnable<Queue<?>> cir) {
		if (!RENDER_ORDER.contains(particleRenderType)) {
			if (!(RENDER_ORDER instanceof ArrayList)) {
				RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
			}
			RENDER_ORDER.add(particleRenderType);
		}
	}
}
