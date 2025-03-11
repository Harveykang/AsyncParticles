package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon.*;
import static net.minecraft.util.Mth.floor;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon {
	@Shadow
	public abstract int getLightColor(float partialTick);
	@Unique
	private short asyncParticles$lightCache = INITIAL_LIGHT_CACHE;

	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		short lightCache = asyncParticles$getPackedLight();
		return LightCachedParticleAddon.isLightCacheValid(lightCache)
			? unpackLight((byte) lightCache)
			: original.call(partialTick);
	}

	@Override
	public void asyncParticles$setLight(int light) {
		asyncParticles$lightCache = (short) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	@Override
	public short asyncParticles$getPackedLight() {
		return asyncParticles$lightCache;
	}

	@Override
	public void asyncParticles$refresh() {
		// mark as outdated, we don't set to -1 because -1 was used as a special value for initial cache
		asyncParticles$lightCache |= Short.MIN_VALUE;
		asyncParticles$setLight(getLightColor(0));
	}

	@Override
	public int asyncParticles$invoke_getLightColor(float partialTick) {
		return getLightColor(partialTick);
	}
}
