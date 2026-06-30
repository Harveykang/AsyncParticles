package fun.qu_an.minecraft.asyncparticles.client.mixin.core.animate_tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.AnimateTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
		super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
	}

	@WrapMethod(method = "animateTick")
	public void animateTick(int xt, int yt, int zt, Operation<Void> original) {
		if (!AsyncTickBehavior.getInstance().isTailTick() &&
			ConfigHelper.isAsyncTickParticle()) {
			// don't tick animate if the game is lagging
			return;
		}
		if (ConfigHelper.isAsyncAnimateTick()) {
			AsyncTickBehavior.getInstance().getTickTaskManager().addTask(() -> original.call(xt, yt, zt));
		} else {
			original.call(xt, yt, zt);
		}
	}

	@Inject(method = "animateTick", at = @At(value = "HEAD"))
	public void onAnimateTickHead(CallbackInfo ci) {
		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
		if (cameraEntity == null) {
			return;
		}
		AnimateTickBehavior.CULL_UNDERWATER_PARTICLE_TYPE.set(!ConfigHelper.isCullUnderwaterParticleType() ||
			cameraEntity.level().getFluidState(cameraEntity.blockPosition()).is(FluidTags.WATER));
	}

	@Inject(method = "animateTick", at = @At(value = "INVOKE", ordinal = 0,
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;doAnimateTick(IIIILnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos$MutableBlockPos;)V"),
		cancellable = true)
	public void onAnimateTick(CallbackInfo ci) {
		if (AsyncTickBehavior.getInstance().isCancelled() && !ConfigHelper.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
