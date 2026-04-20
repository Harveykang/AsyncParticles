package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.ParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.Predicate;

public class GpuParticleBehavior {
	public static final GpuParticleBehavior INSTANCE = new GpuParticleBehavior();
	public final Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = new Reference2ObjectOpenHashMap<>();
	public final ParticleThreadLocal<Integer> DESTROY_LIGHT_CACHE = new ParticleThreadLocal<>();
	// reuse buffers
	private final Map<ParticleRenderType, IParticleRenderer> renderers = new Reference2ObjectOpenHashMap<>();
	/**
	 * Code adapted from <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/src/main/java/me/jellysquid/mods/sodium/mixin/features/render/particle/ParticleManagerMixin.java#L51">wahfl2/sodium-fabric</a>
	 * <p>
	 * License: <a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/README.md#-license">README.md#-license</a> and <a/><a href="https://github.com/wahfl2/sodium-fabric/blob/16768661afc57ab52e7dd580eb4e2b01373bab16/COPYING.LESSER">LGPL-3.0</a>
	 */
	private final List<Class<? extends Particle>> GPU_PARTICLES;

	{
		try {
			//noinspection unchecked
			GPU_PARTICLES = new ArrayList<>(List.of(
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
	public final String RENDER_METHOD = Mappings.getRenderMethod();

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
//		if (GLCaps.csSupport.isSupported()) return new AdvancedParticleRenderer();
		if (GLCaps.tfSupport.isSupported()) return new ParticleRenderer();
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
}
