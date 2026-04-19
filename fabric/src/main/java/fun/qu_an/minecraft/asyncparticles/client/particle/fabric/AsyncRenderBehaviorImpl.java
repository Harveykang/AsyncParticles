package fun.qu_an.minecraft.asyncparticles.client.particle.fabric;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;

@SuppressWarnings("unused")
public class AsyncRenderBehaviorImpl extends AsyncRenderBehavior {
	private static final AsyncRenderBehavior INSTANCE = new AsyncRenderBehaviorImpl();

	public static AsyncRenderBehavior getInstance() {
		return INSTANCE;
	}

	public void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;
	}

	public void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		onTranslucent(mc);

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;

		postTranslucent(mc);
	}
}
