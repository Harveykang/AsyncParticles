package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TickParticleRecursiveAction;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Queue;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<Particle> particles;
	@Unique
	private volatile boolean asyncparticles$shouldRemoveAdditionally;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	public abstract boolean isEmpty();

	@Shadow
	@Final
	protected ParticleEngine engine;

	@WrapOperation(method = "<init>", at = @At(value = "NEW", remap = false,
		target = "(I)Ljava/util/ArrayDeque;"))
	private ArrayDeque<?> redirectNewQueue(int numElements, Operation<ArrayDeque<?>> original) {
		return null;
	}

	@Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/ParticleGroup;particles:Ljava/util/Queue;"))
	private void injectNewQueue(ParticleEngine engine, CallbackInfo ci) {
		particles = ParticleHelper.newParticleQueue();
	}

	@Inject(method = "tickParticles", at = @At("HEAD"))
	public void injectTickParticlesHead(CallbackInfo ci) {
		this.asyncparticles$shouldRemoveAdditionally = false;
	}

	@Inject(method = "tickParticles", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/particle/ParticleGroup;tickParticle(Lnet/minecraft/client/particle/Particle;)V"))
	public void injectTickParticlesTick(CallbackInfo ci, @Local(ordinal = 0) Particle particle) {
		((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
	}

	@Override
	public void asyncparticles$tickParticles(boolean isGpu) {
		if (!isGpu) {
			this.asyncparticles$shouldRemoveAdditionally = true;
		}
		if (isEmpty()) {
			return;
		}
		Queue<? extends Particle> particles = isGpu ? ((GpuParticleGroup) this).asyncparticles$getGpuParticles() : this.particles;
		boolean isAsync = ThreadUtil.isOnParticleTickerThread();
		if (isAsync && ConfigHelper.isSplitParticleTick()) {
			// assert this instanceof AsyncTickableParticleGroup
			TickParticleRecursiveAction.execute((ParticleGroup<?>) (Object) this, particles.spliterator(), isGpu);
			return;
		}
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			boolean isSyncParticle = isAsync && asyncparticles$isSyncParticle(particle);
			// Skip the first tick after the particle is added to the queue.
			// GPU particles don't skip the first tick, but skip the first refresh.
			// skip the first refresh will fix black destruction gpu particles.
			boolean shouldTick = !isSyncParticle && (!particleAddon.asyncparticles$isTicked() || isGpu);
			if (shouldTick) {
				try {
					tickParticle(particle);
				} catch (Throwable t) {
					if (AsyncTickBehavior.getInstance().getExceptionHandler().onTickParticleException(particle, t)) {
						break;
					}
				}
				particleAddon.asyncparticles$setTicked();
			}
			if (!isSyncParticle) {
				((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
			}
		}
	}

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "intValue=16384"))
	private int modifyParticleLimit1(int original) {
		return ConfigHelper.getParticleLimit();
	}

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "intValue=12288"))
	private int modifyParticleLimit2(int original) {
		int particleLimit = ConfigHelper.getParticleLimit();
		return (particleLimit >> 1) + (particleLimit >> 2);
	}

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "floatValue=4096.0"))
	private float modifyParticleLimit3(float original) {
		return ConfigHelper.getParticleLimit() >> 2;
	}

	@Override
	public void asyncparticles$removeDeadParticles() {
		if (asyncparticles$shouldRemoveAdditionally) {
			AsyncTickBehavior.getInstance().doParticlesRemoveIf(particles);
		}
		if (this instanceof GpuParticleGroup gpuParticleGroup) {
			gpuParticleGroup.asyncparticles$removeDeadGpuParticles();
		}
	}
}
