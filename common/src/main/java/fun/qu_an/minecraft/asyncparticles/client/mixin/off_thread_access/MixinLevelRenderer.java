package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class MixinLevelRenderer {
	@WrapMethod(method = "setSectionDirty(IIIZ)V")
	public void setSectionDirty(int x, int y, int z, boolean reRenderOnMainThread, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(x, y, z, reRenderOnMainThread);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(x, y, z, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V")
	public void setBlockDirty(BlockPos pos, boolean reRenderOnMainThread, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(pos, reRenderOnMainThread);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(pos, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlocksDirty")
	public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(minX, minY, minZ, maxX, maxY, maxZ);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(minX, minY, minZ, maxX, maxY, maxZ));
		}
	}

	@WrapMethod(method = "setSectionDirtyWithNeighbors")
	public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(sectionX, sectionY, sectionZ);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(sectionX, sectionY, sectionZ));
		}
	}

	@WrapMethod(method = "destroyBlockProgress")
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(breakerId, pos, progress);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(breakerId, pos, progress));
		}
	}
}
