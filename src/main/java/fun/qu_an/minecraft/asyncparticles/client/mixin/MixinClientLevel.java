package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1001)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
	}

	@Override
	public void addBlockEntityTicker(@NotNull TickingBlockEntity tickingBlockEntity) {
		if (!AsyncTicker.shouldAsyncBlockEntityTick()) {
			super.addBlockEntityTicker(tickingBlockEntity);
			return;
		}
		synchronized (pendingBlockEntityTickers) {
			this.pendingBlockEntityTickers.add(tickingBlockEntity);
		}
	}

	@Override
	protected void tickBlockEntities() {
		if (!AsyncTicker.shouldAsyncBlockEntityTick()) {
			super.tickBlockEntities();
			return;
		}
		if (!AsyncTicker.shouldTickParticles) {
			return;
		}
		ProfilerFiller profilerFiller = this.getProfiler();
		profilerFiller.push("blockEntities");

		// this is more compatible with mixins
		AsyncTicker.BLOCK_ENTITY_OPERATIONS.add(super::tickBlockEntities);
		profilerFiller.pop();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void onInit(CallbackInfo ci) {
		this.random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
	}

	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (AsyncTicker.shouldTickParticles) {
			AsyncTicker.BLOCK_ENTITY_OPERATIONS.add(() -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.isCancelled() && !AsyncTicker.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
