package fun.qu_an.minecraft.asyncedparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncedparticles.client.Caches;
import fun.qu_an.minecraft.asyncedparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
	@Shadow @Final private Queue<Particle> particlesToAdd;

	@Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit)).add(particle);
			}
		}

		particles.values().forEach(particles1 -> particles1.removeIf(particle1 -> !particle1.isAlive()));

		Caches.particlesOperation = original;

	}

	@Inject(method = "reload", at = @At(value = "RETURN"), cancellable = true)
	public void reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		cir.setReturnValue(cir.getReturnValue().thenRun(() -> {
			try {
				SimplePropertiesConfig.load();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).exceptionally(t -> {
			Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty(t.getMessage()));
			return null;
		}));
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z", ordinal = 1))
	public boolean modifyIsEmpty(boolean original) {
		return true;
	}

	@Redirect(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
	public void redirectRemove(Iterator instance) {
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
	public void tickTrackingEmitter(CallbackInfo ci) {
		if (Caches.particlesOperationCancelled) {
			ci.cancel();
			Caches.particlesOperation = null;
		}
	}

	@Inject(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
	public void tickParticleList(CallbackInfo ci) {
		if (Caches.particlesOperationCancelled) {
			ci.cancel();
			Caches.particlesOperation = null;
		}
	}

	@Inject(method = "tickParticleList", at = @At(value = "HEAD"), cancellable = true)
	public void tickParticleListHead(CallbackInfo ci) {
		if (Caches.particlesOperationCancelled) {
			ci.cancel();
			Caches.particlesOperation = null;
		}
	}
}
