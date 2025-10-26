package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({AirFlowParticle.class, AirParticle.class, SteamJetParticle.class})
public abstract class MixinParticle_LightCache
	extends fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticle_LightCache {
	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return asyncparticles$isEnabledLightCache()
			? asyncparticles$getCachedLight()
			: original.call(partialTick);
	}

	@Override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = SHARED_POS.get().set(x, y, z);
		int light;
		try {
			light = level.isLoaded(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			light = 0;
		}
		asyncparticles$setLight(light);
	}
}
