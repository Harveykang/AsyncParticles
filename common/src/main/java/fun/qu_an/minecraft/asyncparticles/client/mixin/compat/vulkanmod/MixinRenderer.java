package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.vulkanmod.RendererAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import net.vulkanmod.vulkan.Renderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(Renderer.class)
public class MixinRenderer implements RendererAddon {
	@Shadow
	private ArrayList<Long> inFlightFences;

	@Shadow
	public static int getFramesNum() {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Unique
	private long asyncparticles$actualFrame;

	@Inject(method = "submitFrame", require = 1,
		at = @At(value = "FIELD", target = "Lnet/vulkanmod/vulkan/Renderer;currentFrame:I", opcode = Opcodes.PUTSTATIC))
	private void onSubmitFrame(CallbackInfo ci) {
		asyncparticles$actualFrame++;
	}

	@Inject(method = "recreateSwapChain", at = @At(value = "FIELD", target = "Lnet/vulkanmod/vulkan/Renderer;currentFrame:I", opcode = Opcodes.PUTSTATIC))
	private void onRecreateSwapChain(CallbackInfo ci) {
		asyncparticles$actualFrame = 0;
	}

	@Inject(method = "recreateSwapChain", at = @At(value = "INVOKE", target = "Lnet/vulkanmod/vulkan/shader/Pipeline;recreateDescriptorSets(I)V"))
	private void onRecreateSwapChain2(CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles()) {
			GpuParticleBehavior.getInstance().getOrCreateRenderer().reset();
		}
	}

	@Override
	public long asyncparticles$getActualFrame() {
		return asyncparticles$actualFrame;
	}

	@Override
	public long asyncparticles$getInFlightFence(long frame) {
		return inFlightFences.get((int) (frame % getFramesNum()));
	}
}
