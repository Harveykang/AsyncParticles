package fun.qu_an.minecraft.asyncparticles.client.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;

@SuppressWarnings("unused")
public class AsyncRendererImpl {
	public static void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderBehavior.INSTANCE.renderAsync = isAsync;
		AsyncRenderBehavior.INSTANCE.particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, t -> !t.isTranslucent());
		AsyncRenderBehavior.INSTANCE.renderAsync = false;
		AsyncRenderBehavior.INSTANCE.particlePhase = false;
	}

	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderBehavior.INSTANCE.onTranslucent(mc);

		AsyncRenderBehavior.INSTANCE.renderAsync = isAsync;
		AsyncRenderBehavior.INSTANCE.particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, ParticleRenderType::isTranslucent);
		AsyncRenderBehavior.INSTANCE.renderAsync = false;
		AsyncRenderBehavior.INSTANCE.particlePhase = false;

		AsyncRenderBehavior.INSTANCE.postTranslucent(mc);
	}
}
