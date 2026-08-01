package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.GlTfParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan.VkCompParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GpuParticleBehavior {
	public static final String RENDER_METHOD = Mappings.getRenderMethod();
	public static final String RENDER_ROTATED_QUAD_METHOD_1 = Mappings.getRenderRotatedQuadMethod1();
	public static final String RENDER_ROTATED_QUAD_METHOD_2 = Mappings.getRenderRotatedQuadMethod2();
	private static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	public IParticleRenderer renderer;
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private final List<Class<? extends Particle>> GPU_PARTICLE_CLASSES;
	private float partialTick;
	//	private Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());
	private int limitMultiplier = 1;

	{
		GPU_PARTICLE_CLASSES = new ReferenceArrayList<>(List.of(
			SingleQuadParticle.class,
			FlyTowardsPositionParticle.class,
			FireworkParticles.OverlayParticle.class,
			FireworkParticles.SparkParticle.class,
			DustColorTransitionParticle.class
		));
	}

	private final Reference2BooleanMap<Class<? extends SingleQuadParticle>> CAN_RENDER_FAST_CACHE =
		new Reference2BooleanOpenHashMap<>();
	private final Map<Class<? extends SingleQuadParticle>, Boolean> CAN_RENDER_FAST_CACHE_OFF_THREAD =
		new ConcurrentHashMap<>();
	private Vec3 perTickCameraPos = Vec3.ZERO;
	private int particleLimit = AsyncParticlesConfig.MIN_PARTICLE_LIMIT;

	public static void init() {
	}

	public static GpuParticleBehavior getInstance() {
		return INSTANCE;
	}

	public void flushBufferAndSwap() {
		ThreadUtil.assertOnMainThread();
		if (renderer != null) {
			renderer.flushBufferAndSwap(getPerTickCameraPos());
		}
	}

	public Vec3 getPerTickCameraPos() {
		return perTickCameraPos;
	}

//	public Vec3 getPrevCameraPos() {
//		return prevCameraPos;
//	}

	public boolean canRenderFast(Particle particle) {
		return particle instanceof SingleQuadParticle sqp && canRenderFast(sqp);
	}

	@SuppressWarnings("unchecked")
	@Unique
	public boolean canRenderFast(SingleQuadParticle sqp) {
		if (sqp.getFacingCameraMode() != SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ) {
			return false;
		}
		if (ThreadUtil.isOnMainThread()) {
			return CAN_RENDER_FAST_CACHE.computeIfAbsent(((ParticleAddon) sqp).asyncparticles$getRealClass(), this::canRenderFast0);
		}
		return CAN_RENDER_FAST_CACHE_OFF_THREAD.computeIfAbsent(sqp.getClass(), k1 -> {
			boolean b1 = canRenderFast0(k1);
			ThreadUtil.enqueueClientTask(() -> CAN_RENDER_FAST_CACHE.put(k1, b1));
			return b1;
		});
	}

	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L180">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	@Unique
	public boolean canRenderFast0(Class<? extends SingleQuadParticle> k) {
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
	public void initRendering() {
		int particleLimit = ConfigHelper.getParticleLimit();
		limitMultiplier = 1;
		if (particleLimit != this.particleLimit) {
			this.particleLimit = particleLimit;
			if (renderer != null) {
				renderer.resize(particleLimit);
			}
		}
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
//		frustum = new Frustum(camera.getCullFrustum()).offset(-3);
		perTickCameraPos = camera.position();
	}

	@ApiStatus.Internal
	public void setUpNextTickRendering(int actualCount) {
		int particleLimit = ConfigHelper.getParticleLimit();
		if (actualCount > particleLimit) {
			limitMultiplier = (actualCount + particleLimit - 1) / particleLimit;
		}
		particleLimit *= limitMultiplier;
		if (particleLimit != this.particleLimit) {
			this.particleLimit = particleLimit;
			if (renderer != null) {
				renderer.resize(particleLimit);
			}
		}
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
//		frustum = new Frustum(camera.getCullFrustum()).offset(-3);
		perTickCameraPos = camera.position();
	}

	public float getPartialTicks() {
		return partialTick;
	}

	public void beginFrame(float deltaPartialTick) {
		this.partialTick = deltaPartialTick;
		if (renderer != null) {
			renderer.beginFrame(deltaPartialTick);
		}
	}

	public IParticleRenderer createRenderer() {
		if (Backends.isVk()) {
			return new VkCompParticleRenderer(ConfigHelper.getParticleLimit());
		}
		if (Backends.isGl()) {
			if (Backends.glTf.isSupported()) {
				return new GlTfParticleRenderer(ConfigHelper.getParticleLimit());
			}
		}
		throw new IllegalStateException("No compatible particle renderer found");
	}

	public void onClearParticles() {
		limitMultiplier = 1;
		particleLimit = ConfigHelper.getParticleLimit();
		if (renderer != null) {
			renderer.resize(ConfigHelper.getParticleLimit());
			renderer.reset();
		}
	}

	public void compute() {
		if (renderer == null || renderer.isShouldSkip()) {
			return;
		}
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		float partialTicks = getPartialTicks();
		renderer.compute(camera, partialTicks);
	}

	public ComputeResult ensureComputeReady() {
		if (renderer == null || renderer.isShouldSkip()) {
			return null;
		}
		return renderer.awaitCompute();
	}

	public IParticleRenderer getOrCreateRenderer() {
		if (!ConfigHelper.isGpuParticles()) {
			throw new IllegalStateException("GPU particle acceleration is not enabled");
		}
		return renderer == null ? renderer = createRenderer() : renderer;
	}

	public void onAddGpu(SingleQuadParticle particle) {
//		assert ConfigHelper.isGpuParticles();
		if (ConfigHelper.isAppendNewParticlesToRenderer()) {
			getOrCreateRenderer().append(getPerTickCameraPos(), particle);
		}
	}

//	public Frustum getFrustum() {
//		return frustum;
//	}

	public int getParticleLimit() {
		return particleLimit;
	}

	public void close() {
		if (renderer != null) {
			renderer.close();
			renderer = null;
		}
	}
}
