package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.iris;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleGroupRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticlesRenderState.class, priority = 1500)
public class MixinParticlesRenderState {
	@TargetHandler(
		mixin = "net.irisshaders.iris.mixin.fantastic.MixinParticlesRenderState",
		name = "submitWithoutItems"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("RETURN"))
	private void onSubmitWithoutItems(SubmitNodeStorage submitNodeStorage, CameraRenderState camera, CallbackInfo ci) {
		if (ConfigHelper.isGpuParticles() && !GpuParticleBehavior.getInstance().getOrCreateRenderer().isShouldSkip()) {
			submitNodeStorage.submitParticleGroup(GpuParticleGroupRenderer.getInstance());
		}
	}
}
