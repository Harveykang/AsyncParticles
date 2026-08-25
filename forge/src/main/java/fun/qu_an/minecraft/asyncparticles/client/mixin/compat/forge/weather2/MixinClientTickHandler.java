package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.weather2;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import weather2.ClientTickHandler;

@Mixin(value = ClientTickHandler.class, remap = false)
public class MixinClientTickHandler {
	@WrapMethod(method = "tick")
	private static void tick(TickEvent.ClientTickEvent event, Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()
			&& AsyncTickBehavior.getInstance().isTailTick()) {
			AsyncTickBehavior.getInstance().addTaskEnsureLevelRunning(
				() -> original.call(event), ExceptionUtil::toThrowDirectly);
		} else {
			original.call(event);
		}
	}
}
