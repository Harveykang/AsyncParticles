package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.VKCaps;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.ParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public class GpuParticleBehavior {
	public static final String RENDER_METHOD = Mappings.getRenderMethod();
	public static final String RENDER_ROTATED_QUAD_METHOD_1 = Mappings.getRenderRotatedQuadMethod1();
	public static final String RENDER_ROTATED_QUAD_METHOD_2 = Mappings.getRenderRotatedQuadMethod2();
	public static final ParticleThreadLocal<Boolean> IS_GPU_PHASE = ParticleThreadLocal.withInitial(RenderSystem::isOnRenderThread, () -> false);
	private static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	public final Map<ParticleRenderType, IParticleRenderer> renderers = new Reference2ReferenceOpenHashMap<>();
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private final List<Class<? extends Particle>> GPU_PARTICLE_CLASSES;
	private float partialTick;
	private final Map<ParticleRenderType, ParticleRenderType> renderTypes = new Object2ObjectArrayMap<>();

	{
		try {
			//noinspection unchecked
			GPU_PARTICLE_CLASSES = new ReferenceArrayList<>(List.of(
				SingleQuadParticle.class,
				FlyTowardsPositionParticle.class,
				FireworkParticles.OverlayParticle.class,
				(Class<? extends Particle>) Class.forName(Mappings.getFireworkSparkClass()),
				DustColorTransitionParticle.class
			));
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Reference2BooleanOpenHashMap<Class<? extends SingleQuadParticle>> CAN_RENDER_FAST_CACHE =
		new Reference2BooleanOpenHashMap<>();
	private Vec3 cameraPos = Vec3.ZERO;
	//	private Vec3 prevCameraPos = Vec3.ZERO;

	//	public final String TICK_METHOD = FabricLoader.getInstance().getMappingResolver().mapMethodName(
//		"intermediary",
//		"net.minecraft.class_703",
//		"method_3070",
//		"()V"
//	);
	private int particleLimit = AsyncParticlesConfig.MIN_PARTICLE_LIMIT;

	public static void init() {
	}

	public static GpuParticleBehavior getInstance() {
		return INSTANCE;
	}

	public void swapAllBuffers() {
		RenderSystem.assertOnRenderThread();
		renderers.values().forEach(renderer -> renderer.unmapBufferAndSwap(getCameraPos()));
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
	public boolean canRenderFast(SingleQuadParticle sqp) {
		if (sqp.getFacingCameraMode() != SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ) {
			return false;
		}
		return CAN_RENDER_FAST_CACHE.computeIfAbsent(sqp.getClass(), (Predicate<Class<? extends SingleQuadParticle>>)
			k -> {
				try {
					Class<?> renderMethodDeclaringClass = k.getMethod(RENDER_METHOD,
						QuadParticleRenderState.class,
						Camera.class,
						float.class).getDeclaringClass();
					Class<?> renderRotatedQuadMethod1DeclaringClass = findDeclaringClass(k,
						RENDER_ROTATED_QUAD_METHOD_1,
						QuadParticleRenderState.class,
						Camera.class,
						Quaternionf.class,
						float.class);
					Class<?> renderRotatedQuadMethod2DeclaringClass = findDeclaringClass(k,
						RENDER_ROTATED_QUAD_METHOD_2,
						QuadParticleRenderState.class,
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
	public void setUpNextTickRendering(int particleLimit) {
		if (particleLimit != this.particleLimit) {
			this.particleLimit = particleLimit;
			renderers.values().forEach(group -> group.resize(particleLimit));
		}
		cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
	}

	public float getPartialTicks() {
		return partialTick;
	}

	public void setPartialTick(float deltaPartialTick) {
		this.partialTick = deltaPartialTick;
	}

	public void beginFrame() {
		renderers.values().forEach(IParticleRenderer::beginFrame);
	}

	public IParticleRenderer createRenderer() {
		if (BackendCaps.glTfSupport.isSupported()) return new ParticleRenderer(ConfigHelper.getParticleLimit());
		throw new IllegalStateException("No compatible particle renderer found");
	}

	public void onClearParticles() {
		renderers.values().forEach(IParticleRenderer::reload);
	}

	public ComputeResult compute() {
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		float partialTicks = getPartialTicks();
		IParticleRenderer renderer = renderers.get(ParticleRenderType.SINGLE_QUADS);
		if (renderer == null || renderer.isShouldSkip()) {
			return null;
		}
		return renderer.compute(camera, partialTicks);
	}

	public IParticleRenderer getOrCreateRenderer(ParticleRenderType particleRenderType) {
		return renderers.computeIfAbsent(particleRenderType, _ -> createRenderer());
	}

	public void onAdd(SingleQuadParticle particle) {
		if (ConfigHelper.isAppendNewParticlesToRenderer()) {
			getOrCreateRenderer(particle.getGroup()).append(getCameraPos(), particle);
		}
	}

	public boolean supportsGpuAcceleration() {
		String backendName = RenderSystem.getDevice().getDeviceInfo().backendName().toLowerCase(Locale.ROOT);
		if (backendName.contains("vulkan")) {
			return false;
		}
		if (backendName.contains("opengl")){
			return BackendCaps.supportsGpuAcceleration();
		}
		throw new IllegalStateException("Unknown backend: " + backendName);
	}
}
