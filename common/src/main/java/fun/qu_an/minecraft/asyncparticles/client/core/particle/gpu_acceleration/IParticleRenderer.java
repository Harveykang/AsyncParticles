package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals.ImmersivePortalsCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particle_core.ParticleCoreCompat;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ParticleVertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11C;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public interface IParticleRenderer extends Closeable {
	static boolean shouldRenderParticle(GpuParticleAddon gpuParticle, Vec3 camPos) {
		return gpuParticle.asyncparticles$shouldRender()
			&& (!ModListHelper.IMMERSIVE_PORTALS_LOADED || ImmersivePortalsCompat.shouldRenderParticle(gpuParticle))
			&& (!ModListHelper.PARTICLE_CORE_LOADED || ParticleCoreCompat.shouldRenderParticle(gpuParticle, camPos));
	}

	void beginFrame(float deltaPartialTick);

	/**
	 * Called per tick.
	 * Called on main thread.
	 */
	void flushBufferAndSwap(Vec3 prevGpuCamPos);

	/**
	 * Called per tick.
	 * Called on main thread.
	 */
	void prepareBuffer();

	boolean isMapped();

	boolean isShouldSkip();

	/**
	 * Called per tick.
	 * Called on non-main thread.
	 */
	<T extends Collection<TextureSheetParticle>> void tick(Vec3 cameraPos, Map<ParticleRenderType, T> particles);

	/**
	 * Appends a new particle to the rendering buffer.
	 * Must be called after tick().
	 * Must be called on main thread.
	 */
	void append(Vec3 cameraPos, TextureSheetParticle tsp);

	/**
	 * Called multiple per frame, but only computed once.
	 * Called on main thread.
	 */
	void compute(Camera camera, float partialTicks);

	/**
	 * Ensures the previously submitted compute dispatch has completed.
	 * Called on the render thread, right before the GPU particle draw.
	 */
	ComputeResult awaitCompute();

	void render(ParticleRenderType renderType);

	void resize(int particleLimit);

	Collection<ParticleRenderType> getComputeLayers();

	void reset();

	@Override
	void close();
}
