package fun.qu_an.minecraft.asyncparticles.client.mixin.immediatelyfast;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.raphimc.immediatelyfast.feature.core.BatchableBufferSource;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BatchableBufferSource.class, priority = 500)
public abstract class MixinBatchableBufferSource {
	@Dynamic
	@Inject(method = {
		"getBuffer",
		"endLastBatch",
		"endBatch*"
	}, at = @At("HEAD"))
	private void getBuffer(CallbackInfoReturnable<VertexConsumer> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Dynamic
	@Inject(method = {
		"close",
		"drawDirect"
	}, remap = false, at = @At("HEAD"))
	private void endBatches(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}
