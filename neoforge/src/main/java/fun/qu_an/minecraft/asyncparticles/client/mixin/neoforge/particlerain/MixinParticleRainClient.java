package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.leclowndu93150.particlerain.ParticleRainClient;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(value = ParticleRainClient.class, remap = false)
public abstract class MixinParticleRainClient {
	@Shadow
	protected abstract void onClientTick(ClientTickEvent.Post event);

	@Redirect(method = "<init>",
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/bus/api/IEventBus;addListener(Ljava/util/function/Consumer;)V"))
	private void onInit(IEventBus bus, Consumer<IEventBus> listener) {
		AsyncTicker.registerEndTickEvent(() -> onClientTick(null));
	}
}
