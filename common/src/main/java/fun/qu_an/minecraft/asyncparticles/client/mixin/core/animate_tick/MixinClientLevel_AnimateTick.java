package fun.qu_an.minecraft.asyncparticles.client.mixin.core.animate_tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.core.Phase;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel_AnimateTick extends Level {
	protected MixinClientLevel_AnimateTick(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
	}

	@WrapMethod(method = "animateTick")
	public void asyncparticles_animateTick(int i, int j, int k, Operation<Void> original) {
		if (ConfigHelper.isAsyncAnimateTick()
			&& AsyncTickBehavior.getInstance().isTailTick()) {
			AsyncTickBehavior.getInstance().addTaskEnsureLevelRunning(() -> original.call(i, j, k), Diagnostic::errorAsyncAnimateTick, Phase.ANIMATE_TICK);
		} else {
			original.call(i, j, k);
		}
	}

	@WrapOperation(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource redirectRandomSource(Operation<RandomSource> original) {
		return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
	}

	@WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
	private void asyncparticles_shouldSyncAnimateTick(Block block, BlockState state, Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
		if (ThreadUtil.isOnParticleThread()
			&& AsyncTickBehavior.getInstance().shouldSyncAnimateTick(block)) {
			BlockPos immutablePos = pos.immutable();
			RandomSource mainThreadRandom = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
			ThreadUtil.runOnClient(() -> {
				if (Minecraft.getInstance().level == level) {
					original.call(block, state, level, immutablePos, mainThreadRandom);
				}
			});
		} else {
			original.call(block, state, level, pos, random);
		}
	}
}
