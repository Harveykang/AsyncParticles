package fun.qu_an.minecraft.asyncparticles.client.particle.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@SuppressWarnings("unused")
public class AsyncRenderBehaviorImpl extends AsyncRenderBehavior {
	public static AsyncRenderBehavior newInstance() {
		return new AsyncRenderBehaviorImpl();
	}

	public void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		renderAsync = isAsync;
		particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, t -> !t.isTranslucent());
		renderAsync = false;
		particlePhase = false;
	}

	public void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		onTranslucent(mc);

		renderAsync = isAsync;
		particlePhase = true;
		mc.particleEngine.render(lightTexture, camera, f, null, ParticleRenderType::isTranslucent);
		renderAsync = false;
		particlePhase = false;

		postTranslucent(mc);
	}
}
