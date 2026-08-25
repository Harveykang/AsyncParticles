package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.GlTfParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class GpuParticleBehavior {
	public static final String RENDER_METHOD = Mappings.getRenderMethod();
	private static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	public IParticleRenderer renderer;
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	public final Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = new Reference2ReferenceOpenHashMap<>();
	private final List<Class<? extends Particle>> GPU_PARTICLE_CLASSES;
	private float partialTick;
	//	private Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f())
	private int limitMultiplier = 1;

	{
		GPU_PARTICLE_CLASSES = new ReferenceArrayList<>(List.of(
			SingleQuadParticle.class,
			TextureSheetParticle.class,
			FireworkParticles.OverlayParticle.class,
			FireworkParticles.SparkParticle.class,
			DustColorTransitionParticle.class
		));
	}

	private final Reference2BooleanMap<Class<? extends TextureSheetParticle>> CAN_RENDER_FAST_CACHE =
		new Reference2BooleanOpenHashMap<>();
	private final Map<Class<? extends TextureSheetParticle>, Boolean> CAN_RENDER_FAST_CACHE_OFF_THREAD =
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

	public boolean canRenderFast(Particle particle) {
		return particle instanceof TextureSheetParticle sqp && canRenderFast(sqp);
	}

	public boolean canRenderFast(TextureSheetParticle tsp) {
//		if (tsp.getFacingCameraMode() != TextureSheetParticle.FacingCameraMode.LOOKAT_XYZ) {
//			return false;
//		}
		if (ThreadUtil.isOnMainThread()) {
			return CAN_RENDER_FAST_CACHE.computeIfAbsent(((ParticleAddon) tsp).asyncparticles$getRealClass(), this::canRenderFast0);
		}
		return CAN_RENDER_FAST_CACHE_OFF_THREAD.computeIfAbsent(tsp.getClass(), k1 -> {
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
	private boolean canRenderFast0(Class<? extends TextureSheetParticle> k) {
		try {
			Class<?> renderMethodDeclaringClass = k.getMethod(RENDER_METHOD,
				VertexConsumer.class,
				Camera.class,
				float.class).getDeclaringClass();
			return GPU_PARTICLE_CLASSES.contains(renderMethodDeclaringClass);
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
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
//		frustum = new Frustum(camera.getCullFrustum()).offset(-3);
		perTickCameraPos = camera.getPosition();
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
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
//		frustum = new Frustum(camera.getCullFrustum()).offset(-3);
		perTickCameraPos = camera.getPosition();
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
//		if (Backends.isVk()) {
//			return new VkCompParticleRenderer(ConfigHelper.getParticleLimit());
//		}
		if (Backends.isGl()) {
			if (Backends.glTf.isSupported()) {
				return new GlTfParticleRenderer(ConfigHelper.getParticleLimit());
			}
		}
		throw new IllegalStateException("No compatible particle renderer found");
	}

	public void onClearParticles() {
		gpuParticles.values().forEach(queue -> queue.forEach(ParticleHelper::onClearParticle));
		gpuParticles.clear();
		limitMultiplier = 1;
		particleLimit = ConfigHelper.getParticleLimit();
		if (renderer != null) {
			renderer.resize(ConfigHelper.getParticleLimit());
			renderer.reset();
		}
	}

	public void compute(Camera camera, float partialTick) {
		if (renderer == null || renderer.isShouldSkip()) {
			return;
		}
		renderer.compute(camera, partialTick);
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

	public IParticleRenderer getRenderer() {
		return renderer;
	}

	public void onAddGpu(TextureSheetParticle particle) {
//		assert ConfigHelper.isGpuParticles();
		if (ConfigHelper.isAppendNewParticlesToRenderer() && AsyncTickBehavior.getInstance().isTailTick()) {
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
