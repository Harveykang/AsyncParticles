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
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
	private boolean asyncParticles$ticked = true;
	@Unique
	private boolean asyncParticles$renderSync;
	@Unique
	private boolean asyncParticles$tickSync;

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
		if (asyncParticles$ticked) return asyncParticles$ticked = false;
		remove();
		return true;
	}

	@Override
	public void asyncparticles$setTicked() {
		this.asyncParticles$ticked = true;
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return this.asyncParticles$ticked;
	}

	@Override
	public void asyncparticles$setRenderSync() {
		asyncParticles$renderSync = true;
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return asyncParticles$renderSync;
	}

	@Override
	public void asyncparticles$setTickSync() {
		asyncParticles$tickSync = true;
	}

	@Override
	public boolean asyncparticles$isTickSync() {
		return asyncParticles$tickSync;
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(float partialTicks) {
		return this.getBoundingBox().inflate(1.0);
	}
}
