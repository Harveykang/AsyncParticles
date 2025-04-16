package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon.compress;
import static fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon.decompress;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon {
	@Shadow @Final public ClientLevel level;
	@Shadow public double x;
	@Shadow public double y;
	@Shadow public double z;
	@Unique
	private byte asyncParticles$lightCache = INITIAL_LIGHT_CACHE;

	@Shadow
	public int getLightColor(float partialTick) {
		throw new AssertionError();
	}

	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return SimplePropertiesConfig.particleLightCache()
			? decompress(asyncParticles$getCompressedLight())
			: original.call(partialTick);
	}

	@Override
	public void asyncParticles$refresh() {
		// for some particles, light is hard coded, so this is not necessary for all particles
		// see override method in MixinParticle_LightCacheNoRefresh
		// TODO: do we need a better design?
		BlockPos blockPos = BlockPos.containing(x, y, z);
		int light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		asyncParticles$setLight(light);
	}

	@Override
	public void asyncParticles$setLight(int light) {
		asyncParticles$lightCache = compress(light);
	}

	@Override
	public byte asyncParticles$getCompressedLight() {
		return asyncParticles$lightCache;
	}

	@Override
	public int asyncParticles$invoke_getLightColor(float partialTick) {
		return getLightColor(partialTick);
	}
}
