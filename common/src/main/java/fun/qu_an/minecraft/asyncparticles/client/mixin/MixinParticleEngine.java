package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine {
	@Shadow
	public Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Mutable
	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	@Mutable
	public static List<ParticleRenderType> RENDER_ORDER;

	@Shadow @Final public TextureManager textureManager;

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
	public void onTickParticle(Particle particle, CallbackInfo ci, @Local Throwable t) {
		if (SimplePropertiesConfig.isTickAsync()){
			throw ExceptionUtil.toThrowDirectly(t);
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
//		assert AsyncTicker.shouldTickParticles;
		particles.forEach((particleRenderType, queue) -> {
			// submit this task even though the queue is empty
			// we'll add particles later
			ProfilerFiller profiler = this.level.getProfiler();
			profiler.push(particleRenderType.toString());
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
			profiler.pop();
		});

		// submit this task even though the queue is empty
		// we'll add particles later
		AsyncTicker.PARTICLE_OPERATIONS.add(this::asyncparticles$tickEmitters);

		AsyncTicker.waitForCleanUp();

		if (!this.particlesToAdd.isEmpty()) {
			// Write like this to be compatible with e.g. Spectrum mod
			Particle particle;
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTicker.recordSync(particle);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(particle.getRenderType(),
					k -> {
//						EvictingQueue<Particle> queue1 = EvictingQueue.create(SimplePropertiesConfig.limit);
						Queue<Particle> queue1 = new IterationSafeEvictingQueue<>(
							16,
							SimplePropertiesConfig.getLimit(),
							AsyncTicker::onEvicted);
						// fix the first added particle not ticked.
						AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue1));
						// fix not added to RENDER_ORDER
						// e.g. LodestoneParticleRenderType#*#withDepthFade()
						if (!ModListHelper.IS_FORGE &&
							k != ParticleRenderType.NO_RENDER &&
							!RENDER_ORDER.contains(k)) {
							// holy shit, this is definitely a giant of shit
							asyncparticles_Neo$addToOrderList(k);
						}
						return queue1;
					});
				queue.add(particle);
			}
			particlesToAdd.clear();
		}
	}

	@Unique
	private void asyncparticles$tickEmitters() {
		for (TrackingEmitter emitter : this.trackingEmitters) {
			if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
				return;
			}
			if (!emitter.isAlive()) {
				continue;
			}
			if (((ParticleAddon) emitter).asyncparticles$isTickSync()) {
				AsyncTicker.recordSync(emitter);
				continue;
			}
			try {
				emitter.tick();
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(emitter, t);
			}
		}
	}

	@Unique
	private void asyncparticles_Neo$addToOrderList(ParticleRenderType k) {
		// must treat as ImmutableList. forge will use this to order treemap
		List<ParticleRenderType> list = new ArrayList<>(RENDER_ORDER.size() + 1);
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) != AsyncRenderer.EMPTY_FORMAT) {
				list.add(type);
			}
		}
		Pair<VertexFormat.Mode, VertexFormat> pair = AsyncRenderer.getVertexFormatPair(k, textureManager);
		if (pair != AsyncRenderer.EMPTY_FORMAT) {
			list.add(k);
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) == AsyncRenderer.EMPTY_FORMAT) {
				list.add(type);
			}
		}
		if (pair == AsyncRenderer.EMPTY_FORMAT) {
			list.add(k);
		}
		RENDER_ORDER = list;
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private void tickParticleList(Collection<Particle> collection) {
		if (collection.isEmpty()) {
			return;
		}
		for (Particle particle : collection) {
			if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
				return;
			}
			if (!particle.isAlive()) {
				// This is to be compatible with e.g. Figura mod
				// Trust JIT
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isTicked()) {
				// Skip the first tick that the particle is added to the queue.
				if (particle instanceof LightCachedParticleAddon lightCachedParticle
					&& SimplePropertiesConfig.particleLightCache()) {
					lightCachedParticle.asyncparticles$refresh();
				}
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
				AsyncTicker.recordSync(particle);
				continue;
			}
			try {
				tickParticle(particle);
				if (particle instanceof LightCachedParticleAddon lightCachedParticle
					&& SimplePropertiesConfig.particleLightCache()) {
					lightCachedParticle.asyncparticles$refresh();
				}
				((ParticleAddon) particle).asyncparticles$setTicked();
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(particle, t);
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"))
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles && SimplePropertiesConfig.isTickAsync()) {
			particle.remove(); // to compatible with some mods...
			// don't cancel it,
			// otherwise it may cause memory leak with some mods
		} else if (particle instanceof LightCachedParticleAddon lightCachedParticle
				   && SimplePropertiesConfig.particleLightCache()) {
			lightCachedParticle.asyncparticles$refresh();
		}
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void redirectClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(AsyncTicker::onEvicted);
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		trackingEmitters.forEach(AsyncTicker::onEvicted);
		trackingEmitters = new BusyWaitEvictingQueue<>(256, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		AsyncTicker.onParticleEngineClear();
	}

//	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
//	public void onCreateParticle(ParticleOptions particleType, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
//		if (!ModListHelper.CREATE_LOADED && !ModListHelper.VS_LOADED) {
//			return;
//		}
//		ResourceLocation key = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType.getType());
//		if (!SimplePropertiesConfig.getWeatherParticles().contains(key)) {
//			return;
//		}
//		if (ModListHelper.CREATE_LOADED && !CreateCompat.canSpawnWeatherParticle(level, x, y, z)) {
//			cir.setReturnValue(null);
//		}
//		if (ModListHelper.VS_LOADED && !VSCompat.canCreateWeatherParticle(level, x, y, z)) {
//			cir.setReturnValue(null);
//		}
//	}
}
