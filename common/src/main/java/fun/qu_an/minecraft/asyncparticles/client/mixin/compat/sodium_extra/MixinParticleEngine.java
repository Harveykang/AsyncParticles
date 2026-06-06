package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sodium_extra;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = ParticleEngine.class, priority = 1500)
public class MixinParticleEngine {
	@TargetHandler(
		mixin = "me.flashyreese.mods.sodiumextra.mixin.particle.MixinParticleEngine",
		name = "addParticle"
	)
	@Redirect(method = "@MixinSquared:Handler", require = 0, at = @At(value = "INVOKE",
		target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
	private Object addParticle(Map<?, Boolean> instance, Object key, Function<?, ?> mappingFunction) {
		return instance.getOrDefault(key, true);
	}
}
