package fun.qu_an.minecraft.asyncparticles.client.fabric;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
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
		AsyncRenderBehavior.INSTANCE.renderAsync = isAsync;
		AsyncRenderBehavior.INSTANCE.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderBehavior.INSTANCE.renderAsync = false;
		AsyncRenderBehavior.INSTANCE.particlePhase = false;
	}

	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderBehavior.INSTANCE.onTranslucent(mc);

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		} else {
			throw new UnsupportedOperationException("endOpaque");
		}
		AsyncRenderBehavior.INSTANCE.renderAsync = isAsync;
		AsyncRenderBehavior.INSTANCE.particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		AsyncRenderBehavior.INSTANCE.renderAsync = false;
		AsyncRenderBehavior.INSTANCE.particlePhase = false;

		AsyncRenderBehavior.INSTANCE.postTranslucent(mc);
	}
}
