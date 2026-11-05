package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.GlTfParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.particle.EmitterPreset;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.*;
import java.util.stream.Collectors;

public class ParticleStormCompat {
	private static IParticleRenderer renderer;
	private static int capacity;
	private static long generation;
	private static ClientLevel sourceLevel;
	private static Layer renderingLayer;
	static String lastGpuComputeShader = "none";
	private static final Map<ParticleRenderType, Layer> GPU_LAYERS = new Reference2ReferenceOpenHashMap<>();

	private record Layer(List<IMolangParticleInstance> cpuParticles, int gpuCount) {
	}

	public static void afterTick(Map<ParticleRenderType, ? extends Collection<?>> particles, Camera camera) {
		ThreadUtil.assertOnMainThread();
		generation++;
		renderingLayer = null;
		sourceLevel = Minecraft.getInstance().level;
		GPU_LAYERS.clear();
		Map<ParticleRenderType, List<TextureSheetParticle>> snapshot = new Reference2ReferenceOpenHashMap<>();
		int required = 0;
		for (Map.Entry<ParticleRenderType, ? extends Collection<?>> entry : particles.entrySet()) {
			Collection<?> queue = entry.getValue();
			ParticleRenderType renderType = entry.getKey();
			if (!supportedMaterial(renderType) || queue.isEmpty()) {
				continue;
			}
			List<TextureSheetParticle> gpuParticles = null;
			List<IMolangParticleInstance> cpuParticles = null;
			for (Object value : queue) {
				if (!(value instanceof IMolangParticleInstance particle)) {
					continue;
				}
				if (particle.getLevel() == sourceLevel && supportedParticle(particle)) {
					if (gpuParticles == null) {
						gpuParticles = new ReferenceArrayList<>(queue.size());
					}
					gpuParticles.add((TextureSheetParticle) particle);
					if (ModListHelper.FABRIC_PARTICLE_STORM_LOADED) {
						((ParticleStormParticle) particle).asyncparticles$setGpuGeneration(generation);
					}
				} else if (ModListHelper.FORGE_PARTICLE_STORM_LOADED) {
					if (cpuParticles == null) {
						cpuParticles = new ReferenceArrayList<>();
					}
					cpuParticles.add(particle);
				}
			}
			if (gpuParticles == null) {
				continue;
			}
			snapshot.put(renderType, gpuParticles);
			Layer layer = new Layer(cpuParticles == null ? Collections.emptyList() : cpuParticles, gpuParticles.size());
			GPU_LAYERS.put(renderType, layer);
			required += gpuParticles.size();
		}
		if (required == 0) {
			if (renderer != null) {
				renderer.reset();
			}
			return;
		}
		int nextCapacity = Math.max(256, capacity);
		while (nextCapacity < required) {
			nextCapacity *= 2;
		}
		if (renderer == null) {
			ParticleStormPipeline strategy = new ParticleStormPipeline();
			renderer = Backends.isVk()
				? new VkCompParticleRenderer<>(nextCapacity, strategy)
				: new GlTfParticleRenderer<>(nextCapacity, strategy);
		} else if (nextCapacity != capacity) {
			renderer.resize(nextCapacity);
		}
		capacity = nextCapacity;
		renderer.prepareBuffer();
		renderer.tick(camera.getPosition(), snapshot);
		renderer.flushBufferAndSwap(camera.getPosition());
	}

	private static boolean supportedParticle(IMolangParticleInstance particle) {
		if (particle.getClass() != MolangParticleInstance.class) {
			return false;
		}
		ParticleEmitter emitter = particle.getEmitter();
		EmitterPreset preset = emitter.getPreset();
		// An attachment/matrix alone does not transform particles (e.g. TerraCurio's infinite_glow).
		if (!preset.localPosition && !preset.localRotation) {
			return true;
		}
		// Keep active or not-yet-initialized render-time attachment transforms on the CPU.
		return !emitter.isLocalSpace() && particle.getAttached() == null;
	}

	private static boolean supportedMaterial(ParticleRenderType type) {
		return type == ParticleRenderType.PARTICLE_SHEET_OPAQUE || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
			|| type == ParticleRenderType.PARTICLE_SHEET_LIT || type == ParticleRenderType.TERRAIN_SHEET
			|| type == PSGameClient.PARTICLE_ADD || type == PSGameClient.PARTICLE_BLEND;
	}

	public static void beginFrame(float partialTick) {
		renderingLayer = null;
		if (renderer != null) {
			renderer.beginFrame(partialTick);
		}
	}

	public static void compute(Camera camera, float partialTick) {
		if (ConfigHelper.isParticleStormGpuRendering() && renderer != null && !renderer.isShouldSkip()
			&& sourceLevel == Minecraft.getInstance().level) {
			renderer.compute(camera, partialTick);
		}
	}

	public static String debugInfo() {
		if (ModListHelper.FORGE_PARTICLE_STORM_LOADED) {
			return forgeDebugInfo();
		}
		if (ModListHelper.FABRIC_PARTICLE_STORM_LOADED) {
			return fabricDebugInfo();
		}
		return "";
	}

	private static String forgeDebugInfo() {
		ParticleStormEngineAddon engine = (ParticleStormEngineAddon) (Object) MolangParticleEngine.INSTANCE;
		return formatDebugInfo(engine.asyncparticles$getGroupedParticles(), engine.asyncparticles$getParticlesToAdd().size());
	}

	private static String fabricDebugInfo() {
		ParticleEngine engine = Minecraft.getInstance().particleEngine;
		int pending = (int) engine.particlesToAdd.stream().filter(IMolangParticleInstance.class::isInstance).count();
		return formatDebugInfo(engine.particles, pending);
	}

	private static String formatDebugInfo(Map<ParticleRenderType, ? extends Collection<?>> particles, int pending) {
		Map<ParticleRenderType, Layer> selected = ConfigHelper.isParticleStormGpuRendering() ? GPU_LAYERS : Collections.emptyMap();
		Map<ParticleRenderType, Integer> counts = particles.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e
				-> (int) e.getValue().stream().filter(o -> o instanceof IMolangParticleInstance).count()));
		return """
			ParticleStorm particles: %d, pending: %d, emitters: %d,
			ParticleStorm GPU-selected particles: %d, GPU buffer capacity: %d,
			ParticleStorm last GPU compute shader: %s,
			ParticleStorm queues (render type, size, path): %s""".formatted(
			counts.values().stream().mapToInt(Integer::intValue).sum(),
			pending,
			MolangParticleEngine.INSTANCE.totalEmitterCount(),
			selected.values().stream().mapToInt(Layer::gpuCount).sum(),
			capacity,
			lastGpuComputeShader,
			counts.entrySet().stream()
				.map(entry -> {
					Layer layer = selected.get(entry.getKey());
					return layer == null
						? "%s: %d (CPU)".formatted(entry.getKey(), entry.getValue())
						: "%s: %d (GPU: %d, CPU: %d)".formatted(entry.getKey(), entry.getValue(),
						layer.gpuCount(), Math.max(0, entry.getValue() - layer.gpuCount()));
				})
				.collect(Collectors.joining(", ", "[", "]"))
		);
	}

	public static void renderLayer(ParticleRenderType type, Camera camera, float partialTick) {
		renderingLayer = null;
		Layer layer = GPU_LAYERS.get(type);
		if (renderer == null || layer == null || sourceLevel != Minecraft.getInstance().level
			|| RenderSystem.getShader() == null
			|| RenderSystem.getShader().getVertexFormat() != DefaultVertexFormat.PARTICLE) {
			return;
		}
		if (!renderer.isShouldSkip()) {
			if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.PARTICLE_RENDERING) {
				renderer.compute(camera, partialTick);
			}
			if (renderer.awaitCompute() != null) {
				// double-sided billboards.
				boolean cull = Backends.isVk() ? VRenderSystem.cull : GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
				if (cull) {
					RenderSystem.disableCull();
				}
				try {
					renderer.render(type);
					renderingLayer = layer;
				} finally {
					if (cull) {
						RenderSystem.enableCull();
					}
				}
			}
		}
	}

	public static boolean isGpuParticle(Particle particle) {
		return renderingLayer != null && particle instanceof ParticleStormParticle snapshot
			&& snapshot.asyncparticles$getGpuGeneration() == generation;
	}

	public static @Nullable Iterator<IMolangParticleInstance> render(ParticleRenderType type, Camera camera, float partialTick) {
		renderLayer(type, camera, partialTick);
		return renderingLayer == null ? null : renderingLayer.cpuParticles().iterator();
	}

	public static void release() {
		if (!ThreadUtil.isOnMainThread()) {
			ThreadUtil.enqueueClientTask(ParticleStormCompat::release);
			return;
		}
		generation++;
		sourceLevel = null;
		renderingLayer = null;
		lastGpuComputeShader = "none";
		GPU_LAYERS.clear();
		capacity = 0;
	}

	public static void close() {
		release();
		if (renderer != null) {
			renderer.close();
			renderer = null;
		}
	}
}
