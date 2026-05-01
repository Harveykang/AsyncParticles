package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.ParticleRenderer;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.Predicate;

public class GpuParticleBehavior {
	public static final String RENDER_METHOD = Mappings.getRenderMethod();
	public static final String RENDER_ROTATED_QUAD_METHOD_1 = Mappings.getRenderRotatedQuadMethod1();
	public static final String RENDER_ROTATED_QUAD_METHOD_2 = Mappings.getRenderRotatedQuadMethod2();
	public static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	public final Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = new Reference2ObjectOpenHashMap<>();
	// reuse buffers
	private final Map<ParticleRenderType, IParticleRenderer> renderers = new Reference2ObjectOpenHashMap<>();
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private final List<Class<? extends Particle>> GPU_PARTICLE_CLASSES;

	{
		try {
			//noinspection unchecked
			GPU_PARTICLE_CLASSES = new ReferenceArrayList<>(List.of(
				SingleQuadParticle.class,
				TextureSheetParticle.class,
				FireworkParticles.OverlayParticle.class,
				(Class<? extends Particle>) Class.forName(Mappings.getFireworkSparkClass()),
				DustColorTransitionParticle.class
			));
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Reference2BooleanOpenHashMap<Class<? extends TextureSheetParticle>> CAN_RENDER_FAST_CACHE =
		new Reference2BooleanOpenHashMap<>();
	private Vec3 cameraPos = Vec3.ZERO;
	//	private Vec3 prevCameraPos = Vec3.ZERO;

	//	public final String TICK_METHOD = FabricLoader.getInstance().getMappingResolver().mapMethodName(
//		"intermediary",
//		"net.minecraft.class_703",
//		"method_3070",
//		"()V"
//	);
	private int particleLimit = -1;

	public void init() {
	}

	private IParticleRenderer newParticleRenderer() {
		if (GLCaps.tfSupport.isSupported()) return new ParticleRenderer(this);
		throw new IllegalStateException("No compatible particle renderer found");
	}

	public IParticleRenderer getRenderer(ParticleRenderType type) {
		return renderers.get(type);
	}

	public void swapAllBuffers() {
		RenderSystem.assertOnRenderThread();
		renderers.values().forEach(IParticleRenderer::unmapBufferAndSwap);
	}

	public void setCameraPos(Vec3 pos) {
//		prevCameraPos = cameraPos;
		cameraPos = pos;
	}

	public Vec3 getCameraPos() {
		return cameraPos;
	}

//	public Vec3 getPrevCameraPos() {
//		return prevCameraPos;
//	}

	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L180">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	@Unique
	public boolean canRenderFast(TextureSheetParticle tsp) {
		return CAN_RENDER_FAST_CACHE.computeIfAbsent(tsp.getClass(), (Predicate<Class<? extends TextureSheetParticle>>)
			k -> {
				try {
					Class<?> renderMethodDeclaringClass = k.getMethod(RENDER_METHOD,
						VertexConsumer.class,
						Camera.class,
						float.class).getDeclaringClass();
					Class<?> renderRotatedQuadMethod1DeclaringClass = findDeclaringClass(k, RENDER_ROTATED_QUAD_METHOD_1,
						VertexConsumer.class,
						Camera.class,
						Quaternionf.class,
						float.class);
					Class<?> renderRotatedQuadMethod2DeclaringClass = findDeclaringClass(k, RENDER_ROTATED_QUAD_METHOD_2,
						VertexConsumer.class,
						Quaternionf.class,
						float.class,
						float.class,
						float.class,
						float.class);
					return GPU_PARTICLE_CLASSES.contains(renderMethodDeclaringClass)
						&& GPU_PARTICLE_CLASSES.contains(renderRotatedQuadMethod1DeclaringClass)
						&& GPU_PARTICLE_CLASSES.contains(renderRotatedQuadMethod2DeclaringClass);
				} catch (NoSuchMethodException e) {
					return false;
				}
			});
	}

	private static Class<?> findDeclaringClass(Class<?> clazz,
	                                           String methodName,
	                                           Class<?>... parameterTypes) throws NoSuchMethodException {
		while (clazz != null && clazz != Object.class) {
			try {
				return clazz.getDeclaredMethod(methodName, parameterTypes).getDeclaringClass();
			} catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		}
		throw new NoSuchMethodException();
	}

	@ApiStatus.Internal
	public void setGpuParticleLimit(int particleLimit) {
		if (particleLimit != this.particleLimit) {
			this.particleLimit = particleLimit;
			renderers.values().forEach(renderer -> renderer.resize(particleLimit));
		}
	}

	@ApiStatus.Internal
	public int getGpuParticleLimit() {
		return particleLimit;
	}

	@ApiStatus.Internal
	public void compute(Camera camera, float f) {
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.update(camera, f, getPrevCameraPos());
//		}
		renderers.values().forEach(renderer -> {
			if (!renderer.isShouldSkip()) {
				renderer.compute(camera, f);
			}
		});
	}

	@ApiStatus.Internal
	public void initParticleRenderType(ParticleRenderType k) {
		Minecraft.getInstance().particleEngine.particles.computeIfAbsent(k, t -> ParticleHelper.newParticleQueue());
		createRenderer(k);
	}

	private IParticleRenderer createRenderer(ParticleRenderType type) {
		RenderSystem.assertOnRenderThread();
		return renderers.computeIfAbsent(type, k -> newParticleRenderer());
	}

	public Frustum getFrustum() {
		return AsyncRenderBehavior.INSTANCE.getFrustum();
	}

	public void beginFrame() {
		for (IParticleRenderer renderer : renderers.values()) {
			renderer.beginFrame();
		}
	}
}
