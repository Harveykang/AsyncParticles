package fun.qu_an.minecraft.asyncparticles.client.particle.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
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

		AsyncRenderBehavior.getInstance().renderAsync = isAsync;
		AsyncRenderBehavior.getInstance().particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, t -> !t.isTranslucent());
		AsyncRenderBehavior.getInstance().renderAsync = false;
		AsyncRenderBehavior.getInstance().particlePhase = false;
	}

	public void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		AsyncRenderBehavior.getInstance().onTranslucent(mc);

		AsyncRenderBehavior.getInstance().renderAsync = isAsync;
		AsyncRenderBehavior.getInstance().particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, ParticleRenderType::isTranslucent);
		AsyncRenderBehavior.getInstance().renderAsync = false;
		AsyncRenderBehavior.getInstance().particlePhase = false;

		AsyncRenderBehavior.getInstance().postTranslucent(mc);
	}
}
