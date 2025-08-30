package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.Predicate;

public class GpuParticles {
	public static final Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = new Reference2ObjectOpenHashMap<>();
	public static final ParticleThreadLocal<Integer> DESTROY_LIGHT_CACHE = new ParticleThreadLocal<>();
	// reuse buffer helper
	private static final Map<ParticleRenderType, ParticleRenderer> renderers = new Reference2ObjectOpenHashMap<>();
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private static final List<Class<? extends Particle>> GPU_PARTICLES;

	static {
		try {
			//noinspection unchecked
			GPU_PARTICLES = new ArrayList<>(List.of(
				SingleQuadParticle.class,
				TextureSheetParticle.class,
				FireworkParticles.OverlayParticle.class,
				(Class<? extends Particle>) Class.forName("net.minecraft.client.particle.FireworkParticles$SparkParticle"),
				DustColorTransitionParticle.class
			));
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final static Reference2BooleanOpenHashMap<Class<? extends TextureSheetParticle>> CAN_RENDER_FAST_CACHE =
		new Reference2BooleanOpenHashMap<>();
	private static Vec3 cameraPos = Vec3.ZERO;
	//	private static Vec3 prevCameraPos = Vec3.ZERO;
	public static final String RENDER_METHOD;

	static {
		RENDER_METHOD = getRenderMethod();
	}

	//	public static final String TICK_METHOD = FabricLoader.getInstance().getMappingResolver().mapMethodName(
//		"intermediary",
//		"net.minecraft.class_703",
//		"method_3070",
//		"()V"
//	);
	private static int particleLimit = -1;

	public static void init() {
	}

	@ExpectPlatform
	private static String getRenderMethod() {
		throw new AssertionError();
	}

	public static ParticleRenderer createRenderer(ParticleRenderType type) {
		RenderSystem.assertOnRenderThread();
		return renderers.computeIfAbsent(type, k -> new ParticleRenderer());
	}

	public static ParticleRenderer getRenderer(ParticleRenderType type) {
		return renderers.get(type);
	}

	public static void swapAllBuffers() {
		RenderSystem.assertOnRenderThread();
		renderers.values().forEach(ParticleRenderer::unmapBufferAndSwap);
	}

	public static void setCameraPos(Vec3 pos) {
//		prevCameraPos = cameraPos;
		cameraPos = pos;
	}

	public static Vec3 getCameraPos() {
		return cameraPos;
	}

//	public static Vec3 getPrevCameraPos() {
//		return prevCameraPos;
//	}

	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L180">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	@Unique
	@ApiStatus.Internal
	public static boolean canRenderFast(TextureSheetParticle tsp) {
		return CAN_RENDER_FAST_CACHE.computeIfAbsent(tsp.getClass(), (Predicate<Class<? extends TextureSheetParticle>>)
			k -> {
				try {
					Class<?> declaringClass = k.getMethod(RENDER_METHOD,
						VertexConsumer.class,
						Camera.class,
						float.class).getDeclaringClass();
					return GPU_PARTICLES.contains(declaringClass);
				} catch (NoSuchMethodException e) {
					return false;
				}
			});
	}

	@ApiStatus.Internal
	public static void setInternalParticleLimit(int particleLimit) {
		if (particleLimit != GpuParticles.particleLimit) {
			GpuParticles.particleLimit = particleLimit;
			renderers.values().forEach(renderer -> renderer.resize(particleLimit));
		}
	}

	@ApiStatus.Internal
	public static int getParticleLimit() {
		return particleLimit;
	}

	@ApiStatus.Internal
	public static void runTfs(Camera camera, float f) {
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.update(camera, f, getPrevCameraPos());
//		}
		renderers.values().forEach(renderer -> {
			if (!renderer.isShouldSkip()) {
				renderer.runTf(camera, f);
			}
		});
	}
}
