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
	protected abstract void setBlockDirty(BlockPos blockPos, boolean bl);

	@Shadow
	public abstract void setBlocksDirty(int i, int j, int k, int l, int m, int n);

	@Shadow
	public abstract void setSectionRangeDirty(int i, int j, int k, int l, int m, int n);

	@Shadow
	public abstract void destroyBlockProgress(int i, BlockPos blockPos, int j);

	@Shadow
	public abstract void onSectionBecomingNonEmpty(long sectionNode);

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	public void injectSetSectionDirty(int i, int j, int k, boolean bl, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setSectionDirty(i, j, k, bl));
		}
	}

	@Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"), cancellable = true)
	public void injectSetBlockDirty(BlockPos pos, boolean reRenderOnMainThread, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setBlockDirty(pos, reRenderOnMainThread));
		}
	}

	@Inject(method = "setBlocksDirty", at = @At("HEAD"), cancellable = true)
	public void injectSetBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ));
		}
	}

	@Inject(method = "setSectionRangeDirty", at = @At("HEAD"), cancellable = true)
	public void injectSetSectionRangeDirty(int i, int j, int k, int l, int m, int n, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.setSectionRangeDirty(i, j, k, l, m, n));
		}
	}

	@Inject(method = "onSectionBecomingNonEmpty", at = @At("HEAD"), cancellable = true)
	public void injectOnSectionBecomingNonEmpty(long sectionNode, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.onSectionBecomingNonEmpty(sectionNode));
		}
	}

	@Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
	public void injectDestroyBlockProgress(int id, BlockPos pos, int progress, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.destroyBlockProgress(id, pos, progress));
		}
	}
}
