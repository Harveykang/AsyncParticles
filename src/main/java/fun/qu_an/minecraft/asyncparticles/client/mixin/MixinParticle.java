package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	@Shadow
	public abstract void remove();

	@Shadow
	public abstract boolean isAlive();

	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Shadow
	@Final
	public ClientLevel level;
	@Unique
	private boolean asyncParticles$ticked;
	@Unique
	private boolean asyncParticles$renderSync;

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		this.asyncParticles$renderSync = AsyncRenderer.shouldSync(((Particle) (Object) this).getClass());
	}

	@WrapOperation(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V",
		at = @At(value = "INVOKE", target ="Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource onInit(Operation<RandomSource> original) {
		return RandomSource.createNewThreadLocalInstance();
	}

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

	@Override
	public void asyncedParticles$setRenderSync() {
		asyncParticles$renderSync = true;
	}

	@Override
	public boolean asyncedParticles$isRenderSync() {
		return asyncParticles$renderSync;
	}
}
