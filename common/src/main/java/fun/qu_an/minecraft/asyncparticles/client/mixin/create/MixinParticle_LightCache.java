package fun.qu_an.minecraft.asyncparticles.client.mixin.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({AirFlowParticle.class, AirParticle.class, SteamJetParticle.class})
public abstract class MixinParticle_LightCache
	extends fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.MixinParticle_LightCache {
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
		BlockPos blockpos = BlockPos.containing(x, y, z);
		int light = level.isLoaded(blockpos) ? LevelRenderer.getLightColor(level, blockpos) : 0;
		asyncparticles$setLight(light);
	}
}
