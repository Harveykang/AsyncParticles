package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinParticle implements LightCachedParticleAddon, ParticleExtension {
	@Shadow
	@Final
	public ClientLevel level;

	@Shadow
	public double x;

	@Shadow
	public double y;

	@Shadow
	public double z;

	@Shadow
	public double xo;
	@Shadow
	public double yo;
	@Shadow
	public double zo;
	@Unique
	protected WeakReference<SubLevel> asyncparticle$tracingSubLevel = new WeakReference<>(null);

	@Unique
	protected BlockPos asyncparticle$lossSublevelPos;

	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return asyncparticles$calcLight(original.call(partialTick), GameUtil.SHARED_POS.get().set(x, y, z));
	}

	@Override // inject after MixinParticle_LightCache to override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = GameUtil.SHARED_POS.get().set(x, y, z);
		int light = level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(level, blockPos) : 0;
		if (ConfigHelper.fixParticleLightOnSableSublevel()) {
			asyncparticles$setLight(asyncparticles$calcLight(light, blockPos));
		} else {
			asyncparticles$setLight(light);
		}
	}

	@Unique
	protected int asyncparticles$calcLight(int light, BlockPos blockPos) {
		SubLevel subLevel = sable$getTrackingSubLevel();
		if (subLevel != null) {
			if (asyncparticle$tracingSubLevel.get() != subLevel) {
				asyncparticle$tracingSubLevel = new WeakReference<>(subLevel);
			}
			return asyncparticles$clampLight(subLevel, light);
		} else if ((subLevel = asyncparticle$tracingSubLevel.get()) == null) {
			asyncparticle$lossSublevelPos = null;
			return light;
		} else if (asyncparticle$lossSublevelPos == null) {
			asyncparticle$lossSublevelPos = BlockPos.containing(xo, yo, zo);
			return asyncparticles$clampLight(subLevel, light);
		} else if (asyncparticle$lossSublevelPos.distManhattan(blockPos) > 15) {
			return light;
		} else {
			return asyncparticles$clampLight(subLevel, light);
		}
	}

	@Unique
	protected int asyncparticles$clampLight(SubLevel subLevel, int light) {
		Vector3d world = new Vector3d(x, y, z);
		Vector3d transformed = subLevel.logicalPose().transformPositionInverse(world, world);
		BlockPos pos = GameUtil.SHARED_POS.get().set(transformed.x, transformed.y, transformed.z);
		int shipLight = level.hasChunkAt(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
		return Math.max(light & 0xFFFF, shipLight & 0xFFFF) | // max for block, min for sky
			Math.min(light & 0xFFFF0000, shipLight & 0xFFFF0000);
	}
}
