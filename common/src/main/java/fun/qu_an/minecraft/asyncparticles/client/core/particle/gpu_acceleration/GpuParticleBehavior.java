package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.GlTfParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class GpuParticleBehavior {
	public static final String RENDER_METHOD = Mappings.getRenderMethod();
	public static final String RENDER_ROTATED_QUAD_METHOD_1 = Mappings.getRenderRotatedQuadMethod1();
	public static final String RENDER_ROTATED_QUAD_METHOD_2 = Mappings.getRenderRotatedQuadMethod2();
	private static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	private static final ParticleRenderType GPU_SINGLE_QUADS = new ParticleRenderType("gpu_single_quads");
	public IParticleRenderer renderer;
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private final List<Class<? extends Particle>> GPU_PARTICLE_CLASSES;
	private float partialTick;
//	private Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());
	private final Map<ParticleRenderType, ParticleRenderType> renderTypes = new Object2ObjectArrayMap<>();
	private int limitMultiplier = 1;

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
	private Vec3 perTickCameraPos = Vec3.ZERO;
	private int particleLimit = AsyncParticlesConfig.MIN_PARTICLE_LIMIT;

	public static void init() {
	}

	public static GpuParticleBehavior getInstance() {
		return INSTANCE;
	}

	public void flushBufferAndSwap() {
		RenderSystem.assertOnRenderThread();
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
					if (AsyncTickBehavior.getInstance().shouldSync(k)) {
						return false;
					}
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
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
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

	public ParticleRenderType ofRenderType(ParticleRenderType renderType) {
		if (renderType == ParticleRenderType.SINGLE_QUADS) {
			return GPU_SINGLE_QUADS;
		}
		return renderTypes.computeIfAbsent(renderType, _ -> new ParticleRenderType("gpu_" + renderType.name()));
	}

	public IParticleRenderer createRenderer() {
		if (BackendCaps.isGl()) {
			if (BackendCaps.glTfSupport.isTfSupported()) {
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
			renderer.reload();
		}
		GpuParticleGroupRenderer.getInstance().clear();
	}

	public void compute() {
		if (renderer == null || renderer.isShouldSkip()) {
			return;
		}
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
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
		return renderer == null ? renderer = createRenderer() : renderer;
	}

	public void onAdd(SingleQuadParticle particle) {
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
}
