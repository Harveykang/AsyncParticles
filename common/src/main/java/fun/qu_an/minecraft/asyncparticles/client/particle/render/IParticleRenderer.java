package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;

public interface IParticleRenderer {
	void beginFrame();

	void unmapBufferAndSwap();

	void mapBuffer();

	void unmapBuffer();

	boolean isShouldSkip();

	/**
	 * Called per tick.
	 * Can be called on non-main thread.
	 */
	void tick(Vec3 cameraPos, Queue<TextureSheetParticle> particles);

	/**
	 * Called per frame.
	 */
	void compute(Camera camera, float partialTicks);

	/**
	 * Called per frame.
	 */
	void render();

	/**
	 * Appends a new particle to the rendering buffer.
	 * Must be called after tick().
	 * Must be called on main thread.
	 */
	void append(Vec3 cameraPos, TextureSheetParticle tsp);

	void resize(int particleLimit);

	static void prepareShader(ShaderInstance shader) {
		for (int i = 0; i < 12; ++i) {
			int j = RenderSystem.getShaderTexture(i);
			shader.setSampler("Sampler" + i, j);
		}

		if (shader.MODEL_VIEW_MATRIX != null) {
			shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
		}

		if (shader.PROJECTION_MATRIX != null) {
			shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
		}

		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		}

		if (shader.COLOR_MODULATOR != null) {
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}

		if (shader.GLINT_ALPHA != null) {
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
		}

		if (shader.FOG_START != null) {
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		}

		if (shader.FOG_END != null) {
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		}

		if (shader.FOG_COLOR != null) {
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		}

		if (shader.FOG_SHAPE != null) {
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		}

		if (shader.TEXTURE_MATRIX != null) {
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}

		if (shader.GAME_TIME != null) {
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}

		if (shader.SCREEN_SIZE != null) {
			Window window = Minecraft.getInstance().getWindow();
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		}

//		if (shader.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
//			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
//		}

		RenderSystem.setupShaderLights(shader);
	}
}
