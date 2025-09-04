package fun.qu_an.minecraft.asyncparticles.client.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, t -> !t.isTranslucent());
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;
	}

	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderer.onTranslucent(mc);

		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, ParticleRenderType::isTranslucent);
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;

		AsyncRenderer.postTranslucent(mc);
	}
}
