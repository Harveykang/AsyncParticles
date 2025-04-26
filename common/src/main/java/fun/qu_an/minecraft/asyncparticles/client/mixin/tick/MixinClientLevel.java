package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
		super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
	}

	@Override
	public void addBlockEntityTicker(@NotNull TickingBlockEntity tickingBlockEntity) {
		if (!SimplePropertiesConfig.asyncBlockEntityTick()) {
			super.addBlockEntityTicker(tickingBlockEntity);
			return;
		}
		synchronized (pendingBlockEntityTickers) {
			this.pendingBlockEntityTickers.add(tickingBlockEntity);
		}
	}

	@Override
	protected void tickBlockEntities() {
		if (!AsyncTicker.shouldTickParticles ||
			!SimplePropertiesConfig.asyncBlockEntityTick()) {
			super.tickBlockEntities();
			return;
		}
		ProfilerFiller profiler = Profiler.get();
		profiler.push("blockEntities");

		// this is more compatible with mixins
		// See MixinLevel.tickBlockEntities
		AsyncTicker.BLOCK_ENTITY_OPERATIONS.add(super::tickBlockEntities);
		profiler.pop();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void onInit(CallbackInfo ci) {
		if (this.random.getClass() != SingleThreadedRandomSource.class) {
			this.random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
		}
	}

	@Unique
	private static final ResourceLocation asyncparticles$ANIMATE_TICK =
		ResourceLocation.tryBuild("asyncparticles", "animate_tick");
	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (!AsyncTicker.shouldTickParticles &&
			SimplePropertiesConfig.isTickAsync()) {
			// don't tick animate if the game is lagging
			return;
		}
		if (!SimplePropertiesConfig.asyncBlockEntityAnimate()) {
			original.call(i, j, k);
		} else {
			AsyncTicker.addEndTickTask(asyncparticles$ANIMATE_TICK, () -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
