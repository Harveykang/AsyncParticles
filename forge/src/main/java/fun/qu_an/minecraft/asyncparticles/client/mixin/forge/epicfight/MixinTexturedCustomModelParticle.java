package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.epicfight;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.particle.TexturedCustomModelParticle;

@Mixin(TexturedCustomModelParticle.class)
public abstract class MixinTexturedCustomModelParticle implements ParticleAddon {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		asyncedParticles$setRenderSync();
		if (!AsyncRenderer.shouldSync(((Particle)(Object)this).getClass())) {
			AsyncRenderer.markAsSync(((Particle)(Object)this).getClass());
		}
	}
}
