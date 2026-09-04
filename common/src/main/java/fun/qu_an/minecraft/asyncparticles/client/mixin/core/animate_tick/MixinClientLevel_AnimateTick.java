package fun.qu_an.minecraft.asyncparticles.client.mixin.core.animate_tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.core.Phase;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.LevelBundle;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel_AnimateTick {
	@Unique
	private RandomSource asyncparticles$random;

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
			ThreadUtil.runOnClient(() -> {
				// We must use strict checks because level, player, and cameraEntity
				// are not always available at the same time, which can cause crashes.
				if (LevelBundle.isLevelAvailable()) {
					original.call(block, state, level, immutablePos, this.asyncparticles$getRandom());
				}
			});
		} else {
			original.call(block, state, level, pos, random);
		}
	}

	@WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;animateTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
	private void asyncparticles_shouldSyncAnimateTick(FluidState fluidState, Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
		if (ThreadUtil.isOnParticleThread()
			&& AsyncTickBehavior.getInstance().shouldSyncAnimateTick(fluidState.getType())) {
			BlockPos immutablePos = pos.immutable();
			ThreadUtil.runOnClient(() -> {
				if (LevelBundle.isLevelAvailable()) {
					original.call(fluidState, level, immutablePos, this.asyncparticles$getRandom());
				}
			});
		} else {
			original.call(fluidState, level, pos, random);
		}
	}

	@Unique
	private RandomSource asyncparticles$getRandom() {
		// Reuse a main thread only random source to avoid repeated allocation,
		// since doAnimateTick is invoked 1664 times per tick, and we cannot ignore the worst cases.
		RandomSource random = this.asyncparticles$random;
		if (random != null) {
			return random;
		}
		return this.asyncparticles$random = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
	}
}
