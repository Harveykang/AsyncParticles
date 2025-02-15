package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1001)
public abstract class MixinClientLevel extends Level {
	@Unique
	private volatile boolean tickingBlockEntities;
	@Unique
	private static final Object lock = new Object();

	protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
	}

	@Override
	public void addBlockEntityTicker(TickingBlockEntity tickingBlockEntity) {
		synchronized (lock) {
			if (this.tickingBlockEntities) {
				this.pendingBlockEntityTickers.add(tickingBlockEntity);
			} else {
				this.blockEntityTickers.add(tickingBlockEntity);
			}
		}
	}

	@Override
	protected void tickBlockEntities() {
		if (!AsyncTicker.shouldTickParticles) {
			return;
		}
		ProfilerFiller profilerFiller = this.getProfiler();
		profilerFiller.push("blockEntities");
		Runnable runnable = () -> {
			this.tickingBlockEntities = true;
			if (!this.pendingBlockEntityTickers.isEmpty()) {
				synchronized (lock) {
					if (!this.pendingBlockEntityTickers.isEmpty()) {
						this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
						this.pendingBlockEntityTickers.clear();
					}
				}
			}

			Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();

			while (iterator.hasNext()) {
				TickingBlockEntity tickingBlockEntity = iterator.next();
				if (tickingBlockEntity.isRemoved()) {
					iterator.remove();
				} else if (this.shouldTickBlocksAt(tickingBlockEntity.getPos())) {
					tickingBlockEntity.tick();
				}
			}

			this.tickingBlockEntities = false;
		};
		AsyncTicker.beforeParticleOperations.add(runnable);
		profilerFiller.pop();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void onInit(CallbackInfo ci) {
		if (this.random instanceof LegacyRandomSource) {
			this.random = RandomSource.createThreadSafe();
		}
	}

	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (AsyncTicker.shouldTickParticles) {
			AsyncTicker.beforeParticleOperations.add(() -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.cancelled) {
			ci.cancel();
		}
	}
}
