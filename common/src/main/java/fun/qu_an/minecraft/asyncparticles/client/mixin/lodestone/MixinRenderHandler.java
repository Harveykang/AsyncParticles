package fun.qu_an.minecraft.asyncparticles.client.mixin.lodestone;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.ReentrantSpinLock;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.lodestar.lodestone.handlers.RenderHandler;

import java.util.HashMap;

@Mixin(RenderHandler.class)
public class MixinRenderHandler {
	@Unique
	private static final ReentrantSpinLock asyncparticles$lock = new ReentrantSpinLock();

	@WrapMethod(method = "addRenderType")
	private static void wrapWithLock(RenderType renderType, Operation<Void> original) {
		asyncparticles$lock.wrap(() -> original.call(renderType));
	}

	@WrapMethod(method = "renderBufferedBatches(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Ljava/util/HashMap;Z)V")
	private static void renderBufferedBatches(MultiBufferSource.BufferSource bufferSource,
											  HashMap<RenderType, BufferBuilder> buffer,
											  boolean transparentOnly,
											  Operation<Void> original) {
		asyncparticles$lock.wrap(() -> original.call(bufferSource, buffer, transparentOnly));
	}

	@Inject(method = {
		"renderBufferedBatches(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Ljava/util/HashMap;Z)V",
		"beginBufferedRendering",
		"endBufferedRendering",
		"endBatches*",
		"copyDepthBuffer"
	},
		at = @At("HEAD"))
	private static void assertion(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}
