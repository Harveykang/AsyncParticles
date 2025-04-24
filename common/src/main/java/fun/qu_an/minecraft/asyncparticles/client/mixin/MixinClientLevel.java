package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
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
		ProfilerFiller profilerFiller = this.getProfiler();
		profilerFiller.push("blockEntities");

		// this is more compatible with mixins
		// See MixinLevel.tickBlockEntities
		AsyncTicker.BLOCK_ENTITY_OPERATIONS.add(super::tickBlockEntities);
		profilerFiller.pop();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void onInit(CallbackInfo ci) {
		if (this.random.getClass() != SingleThreadedRandomSource.class) {
			this.random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
		}
	}

	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (!AsyncTicker.shouldTickParticles) {
			// don't tick animate if the game is lagging
			return;
		}
		if (!SimplePropertiesConfig.asyncBlockEntityAnimate()) {
			original.call(i, j, k);
		} else {
			AsyncTicker.addEndTickTask(() -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
