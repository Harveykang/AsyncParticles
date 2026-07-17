package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.sable_create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.foundation.particle.AirParticle;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable.MixinParticle;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {AirFlowParticle.class, AirParticle.class, SteamJetParticle.class}, priority = 1500)
// Later than mixin.create.MixinParticle_LightCache
public abstract class MixinParticle_LightCache extends MixinParticle {
	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		int call = original.call(partialTick);
		if (ConfigHelper.fixParticleLightOnSableSublevel()) {
			return asyncparticles$calcLight(call, GameUtil.SHARED_POS.get().set(x, y, z));
		} else {
			return call;
		}
	}

	@Override // inject after MixinParticle_LightCache to override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = GameUtil.SHARED_POS.get().set(x, y, z);
		int light = level.isLoaded(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		if (ConfigHelper.fixParticleLightOnSableSublevel()) {
			asyncparticles$setLight(asyncparticles$calcLight(light, blockPos));
		} else {
			asyncparticles$setLight(light);
		}
	}

	@Override
	@Unique
	protected int asyncparticles$clampLight(SubLevel subLevel, int light) {
		Vector3d world = new Vector3d(x, y, z);
		Vector3d transformed = subLevel.logicalPose().transformPositionInverse(world, world);
		BlockPos pos = GameUtil.SHARED_POS.get().set(transformed.x, transformed.y, transformed.z);
		int shipLight = level.isLoaded(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
		return Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
			Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
	}
}
