package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon.compress;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache implements LightCachedParticleAddon {
	@Shadow
	@Final
	public ClientLevel level;
	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Unique
	private byte asyncparticles$lightCache = INITIAL_LIGHT_CACHE;

	@Shadow
	public int getLightColor(float partialTick) {
		throw new AssertionError();
	}

	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return asyncparticles$isEnabledLightCache()
			? asyncparticles$getCachedLight()
			: original.call(partialTick);
	}

	@Override
	public void asyncparticles$refresh() {
		// for some particles, light is hard coded, so this is not necessary for all particles
		// see override method in MixinParticle_LightCacheNoRefresh
		// TODO: do we need a better design?
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = BlockPos.containing(x, y, z);
		int levelLight;
		try {
			levelLight = LevelRenderer.getLightColor(level, blockPos);
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			levelLight = 0;
		}
		int light = level.hasChunkAt(blockPos) ? levelLight : 0;
		asyncparticles$setLight(light);
	}

	@Override
	public void asyncparticles$setLight(int light) {
		asyncparticles$lightCache = compress(light);
	}

	@Override
	public byte asyncparticles$getCompressedLight() {
		return asyncparticles$lightCache;
	}

	@Override
	public int asyncparticles$invoke_getLightColor(float partialTick) {
		try {
			return getLightColor(partialTick);
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			return 0;
		}
	}
}
