package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.ParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	@Shadow
	public abstract void remove();

	@Shadow
	public abstract boolean isAlive();

	@Unique
	private boolean asyncParticles$ticked;
//	@Unique
//	private boolean asyncParticles$renderSync;

//	@Inject(method = "<init>*", at = @At("RETURN"))
//	private void onInit(CallbackInfo ci) {
//		this.asyncParticles$renderSync = AsyncRenderer.shouldSync(((Particle) (Object) this).getClass());
//	}

	@Override
	public boolean asyncParticles$shouldRemove() {
		if (!isAlive()) return true;
		if (asyncParticles$ticked) return asyncParticles$ticked = false;
		remove();
		return true;
	}

	@Override
	public void asyncParticles$setTicked() {
		this.asyncParticles$ticked = true;
	}

	@Override
	public boolean asyncParticles$isTicked() {
		return this.asyncParticles$ticked;
	}

//	@Override
//	public void asyncedParticles$setRenderSync() {
//		asyncParticles$renderSync = true;
//	}
//
//	@Override
//	public boolean asyncedParticles$isRenderSync() {
//		return asyncParticles$renderSync;
//	}
}
