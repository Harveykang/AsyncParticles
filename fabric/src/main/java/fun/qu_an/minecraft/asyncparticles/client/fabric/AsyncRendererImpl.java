package fun.qu_an.minecraft.asyncparticles.client.fabric;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;
	}

	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderer.onTranslucent(mc);

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		AsyncRenderer.renderAsync = isAsync;
		AsyncRenderer.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderer.renderAsync = false;
		AsyncRenderer.particlePhase = false;

		AsyncRenderer.postTranslucent(mc);
	}
}
