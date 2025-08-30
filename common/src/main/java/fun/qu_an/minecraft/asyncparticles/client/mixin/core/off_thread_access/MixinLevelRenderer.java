package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

// Why some of the particles call these methods?
@Mixin(value = LevelRenderer.class, priority = 1100)
@Environment(EnvType.CLIENT)
public abstract class MixinLevelRenderer {
	@WrapMethod(method = "setSectionDirty(IIIZ)V")
	public void setSectionDirty(int x, int y, int z, boolean reRenderOnMainThread, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(x, y, z, reRenderOnMainThread);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(x, y, z, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V")
	public void setBlockDirty(BlockPos pos, boolean reRenderOnMainThread, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(pos, reRenderOnMainThread);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(pos, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlocksDirty")
	public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(minX, minY, minZ, maxX, maxY, maxZ);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(minX, minY, minZ, maxX, maxY, maxZ));
		}
	}

	@WrapMethod(method = "setSectionDirtyWithNeighbors")
	public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(sectionX, sectionY, sectionZ);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(sectionX, sectionY, sectionZ));
		}
	}

	@WrapMethod(method = "destroyBlockProgress")
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(breakerId, pos, progress);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(breakerId, pos, progress));
		}
	}
}
