package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 9999)
public abstract class MixinParticleEngine_Late {
	@Shadow
	@Mutable
	public static List<ParticleRenderType> RENDER_ORDER;

	// Some mod has duplicated render type (render twice), cause concurrent access to the same queue...
	// Only necessary to fabric since forge use particles.keySet()
	@Inject(at = @At("RETURN"), method = "<clinit>")
	private static void addTypes(CallbackInfo ci) {
		RENDER_ORDER = ImmutableList.copyOf(new LinkedHashSet<>(RENDER_ORDER));
	}
}
