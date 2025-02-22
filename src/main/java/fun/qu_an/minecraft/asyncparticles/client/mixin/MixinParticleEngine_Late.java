package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 2000)
public abstract class MixinParticleEngine_Late {
	@Mutable
	@Final
	@Shadow
	private static List<ParticleRenderType> RENDER_ORDER;

	// FIXME: Some mod has duplicated render type, cause concurrent access to the same queue...
	@Inject(at = @At("RETURN"), method = "<clinit>")
	private static void addTypes(CallbackInfo ci) {
		RENDER_ORDER = ImmutableList.<ParticleRenderType>builder().addAll(new LinkedHashSet<>(RENDER_ORDER)).build();
	}
}
