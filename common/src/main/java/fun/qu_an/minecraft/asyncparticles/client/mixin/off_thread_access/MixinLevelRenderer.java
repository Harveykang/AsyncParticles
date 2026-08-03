package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class MixinLevelRenderer {
	@Shadow
	protected abstract void setSectionDirty(int i, int j, int k, boolean bl);

	@Shadow
	protected abstract void setBlockDirty(BlockPos arg, boolean bl);

	@Shadow
	public abstract void setBlocksDirty(int l, int m, int n, int o, int p, int q);

	@Shadow
	public abstract void setSectionDirtyWithNeighbors(int l, int m, int n);

	@Shadow
	public abstract void destroyBlockProgress(int i, BlockPos arg, int j);

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	public void setSectionDirty(int i, int j, int k, boolean bl, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setSectionDirty(i, j, k, bl));
		}
	}

	@Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"), cancellable = true)
	public void setBlockDirty(BlockPos blockPos, boolean bl, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setBlockDirty(blockPos, bl));
		}
	}

	@Inject(method = "setBlocksDirty", at = @At("HEAD"), cancellable = true)
	public void setBlocksDirty(int i, int j, int k, int l, int m, int n, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setBlocksDirty(i, j, k, l, m, n));
		}
	}

	@Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"), cancellable = true)
	public void setSectionDirtyWithNeighbors(int i, int j, int k, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setSectionDirtyWithNeighbors(i, j, k));
		}
	}

	@Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
	public void destroyBlockProgress(int i, BlockPos blockPos, int j, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.destroyBlockProgress(i, blockPos, j));
		}
	}
}
