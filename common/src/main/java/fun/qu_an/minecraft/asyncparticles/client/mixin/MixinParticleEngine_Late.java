package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.ImmutableList;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = ParticleEngine.class, priority = 9000)
public abstract class MixinParticleEngine_Late {
	@Mutable
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Final
	public TextureManager textureManager;

	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Shadow
	public Queue<Particle> particlesToAdd;

	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Mutable
	@Shadow
	@Final
	private RandomSource random;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		trackingEmitters = new BusyWaitEvictingQueue<>(256, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextInt());
		// make custom types render after non-customs
		// Remove duplicated render types, (e.g. Hex Casting mod's bug)
		Set<ParticleRenderType> renderTypes = new LinkedHashSet<>((int) (RENDER_ORDER.size() * 1.34 + 1));
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) != AsyncRenderer.EMPTY_FORMAT) {
				renderTypes.add(type);
			}
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) == AsyncRenderer.EMPTY_FORMAT) {
				renderTypes.add(type);
			}
		}
		RENDER_ORDER = ImmutableList.copyOf(renderTypes);
	}
}
