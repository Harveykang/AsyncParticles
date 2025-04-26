package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.AABB;
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

	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Shadow
	@Final
	public ClientLevel level;

	@Shadow
	public abstract AABB getBoundingBox();

	// TODO: 换 byte?
	@Unique
	private boolean asyncparticles$ticked = true;
	@Unique
	private boolean asyncparticles$renderSync;
	@Unique
	private boolean asyncparticles$tickSync;

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		if (AsyncRenderer.shouldSync(((Particle) (Object) this).getClass())) {
			asyncparticles$setRenderSync();
		}
		if (AsyncTicker.shouldSync(((Particle) (Object) this).getClass())) {
			asyncparticles$setTickSync();
		}
	}

	@WrapOperation(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource onInit(Operation<RandomSource> original) {
		return new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
	}

	@Override
	public boolean asyncparticles$shouldRemove() {
		if (!isAlive()) return true;
		if (asyncparticles$ticked) return asyncparticles$ticked = false;
		remove();
		return true;
	}

	@Override
	public void asyncparticles$setTicked() {
		this.asyncparticles$ticked = true;
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
}
