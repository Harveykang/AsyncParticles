package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create;

import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({AirFlowParticle.class, AirParticle.class, SteamJetParticle.class})
public abstract class MixinParticle_LightCache
	extends fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache.MixinParticle_LightCache {
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
