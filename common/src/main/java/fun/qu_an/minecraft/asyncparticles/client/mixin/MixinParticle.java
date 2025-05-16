package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	@Shadow
	public abstract void remove();

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
	private boolean asyncparticles$ticked = true;
	@Unique
	private boolean asyncparticles$renderSync;
	@Unique
	private boolean asyncparticles$tickSync;

	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
	protected void onInit(CallbackInfo ci) {
		Class<?> aClass = asyncparticles$getRealClass();
		if (AsyncTicker.shouldSync(aClass)) {
			asyncparticles$setTickSync();
		}
		if (AsyncRenderer.shouldSync(aClass)) {
			asyncparticles$setRenderSync();
		}
	}

	@WrapOperation(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource onInit(Operation<RandomSource> original) {
		return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
	}

	@Shadow
	public abstract int getLightColor(float partialTick);

	@Override
	public void asyncparticles$setTicked() {
		this.asyncparticles$ticked = true;
	}

	@Override
	public void asyncparticles$resetTicked() {
		this.asyncparticles$ticked = false;
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return this.asyncparticles$ticked;
	}

	@Override
	public void asyncparticles$setRenderSync() {
		asyncparticles$renderSync = true;
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return asyncparticles$renderSync;
	}

	@Override
	public void asyncparticles$setTickSync() {
		asyncparticles$tickSync = true;
	}

	@Override
	public boolean asyncparticles$isTickSync() {
		return asyncparticles$tickSync;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}
}
