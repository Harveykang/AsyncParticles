package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {AirFlowParticle.class, AirParticle.class, SteamJetParticle.class}, priority = 5050)
public abstract class MixinParticle_LightCache_Late
	extends fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.MixinParticle_LightCache {
	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return asyncparticles$isEnabledLightCache()
			? asyncparticles$getCachedLight()
			: original.call(partialTick);
	}
}
