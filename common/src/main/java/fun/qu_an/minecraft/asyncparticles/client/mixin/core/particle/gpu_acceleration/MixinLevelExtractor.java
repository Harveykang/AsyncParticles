package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {
	@Inject(method = "extract", at = @At("HEAD"))
	private void extractLevelHead(DeltaTracker deltaTracker,
	                              Camera camera,
	                              float deltaPartialTick,
	                              CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles()) {
			GpuParticleBehavior.getInstance().beginFrame(deltaPartialTick);
			if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.LEVEL_EXTRACTION) {
				GpuParticleBehavior.getInstance().compute();
			}
		}
	}
}
