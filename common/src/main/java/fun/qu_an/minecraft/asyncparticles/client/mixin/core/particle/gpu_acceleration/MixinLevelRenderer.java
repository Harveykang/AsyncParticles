package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Inject(method = "extractLevel", at = @At("HEAD"))
	private void extractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
		GpuParticleBehavior.INSTANCE.beginFrame();
		GpuParticleBehavior.INSTANCE.setPartialTick(deltaPartialTick);
	}
}
