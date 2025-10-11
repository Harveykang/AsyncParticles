package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sodium_extra;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.particle.FireworkParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = FireworkParticles.Starter.class, priority = 1500)
public class MixinFireworkParticles$Starter {
	@TargetHandler(
		mixin = "me.flashyreese.mods.sodiumextra.mixin.particle.MixinFireworkParticle",
		name = "addExplosionParticle"
	)
	@Redirect(method = "@MixinSquared:Handler", require = 0, at = @At(value = "INVOKE",
		target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
	private Object addExplosionParticle(Map instance, Object key, Function function) {
		return instance.getOrDefault(key, true);
	}
}
