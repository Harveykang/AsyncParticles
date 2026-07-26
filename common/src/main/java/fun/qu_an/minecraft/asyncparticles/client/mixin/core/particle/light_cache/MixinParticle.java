package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class MixinParticle implements LightCachedParticleAddon {
	@Shadow
	@Final
	protected ClientLevel level;
	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Unique
	private byte asyncparticles$lightCache = INITIAL_LIGHT_CACHE;

	@WrapMethod(method = "getLightColor", order = 15000)
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		if (asyncparticles$isEnabledLightCache()) {
			return asyncparticles$getCachedLight();
		}
		try {
			return original.call(partialTick);
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			return 0;
		}
	}

	@Override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = GameUtil.SHARED_POS.get().set(x, y, z);
		int light;
		try {
			light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			light = 0;
		}
		asyncparticles$setLight(light);
	}

	@Override
	public void asyncparticles$setLight(int light) {
		asyncparticles$lightCache = LightCachedParticleAddon.compress(light);
	}

	@Override
	public int asyncparticles$getCachedLight() {
		return LightCachedParticleAddon.decompress(asyncparticles$lightCache);
	}
}
