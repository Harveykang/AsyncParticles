package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import org.spongepowered.asm.mixin.Mixin;

import static fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon.unpackLight;

@Mixin({AirFlowParticle.class, AirParticle.class, SteamJetParticle.class})
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon {
	@WrapMethod(method = "getLightColor")
	public int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		short lightCache = asyncParticles$getPackedLight();
		return LightCachedParticleAddon.isLightCacheValid(lightCache)
			? unpackLight((byte) lightCache)
			: original.call(partialTick);
	}
}
