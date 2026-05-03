package fun.qu_an.minecraft.asyncparticles.client.mixin.core.weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_TickRain {
	@Unique
	private static final ResourceLocation asyncparticles$TICK_RAIN = GameUtil.id("tick_rain");

	@WrapMethod(method = "tickRain")
	private void wrapTickRain(Camera camera, Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync() && AsyncTickBehavior.INSTANCE.isShouldTickParticles()) {
			EndTickOperation.schedule(asyncparticles$TICK_RAIN, true, () -> original.call(camera));
		} else {
			original.call(camera);
		}
	}
}
