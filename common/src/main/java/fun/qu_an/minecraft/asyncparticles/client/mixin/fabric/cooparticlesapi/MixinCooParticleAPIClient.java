package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.cooparticlesapi;

import cn.coostack.cooparticlesapi.CooParticleAPIClient;
import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmittersManager;
import cn.coostack.cooparticlesapi.network.particle.style.ParticleStyleManager;
import cn.coostack.cooparticlesapi.particles.control.group.ClientParticleGroupManager;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CooParticleAPIClient.class)
public class MixinCooParticleAPIClient {
	@Redirect(method = "onInitializeClient", remap = false,
		at = @At(value = "INVOKE", remap = false, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private void onRegisterEvent(Event<?> instance, Object t) {
		if (t instanceof ClientTickEvents.StartWorldTick event) {
			AsyncTicker.registerEndTickEvent(ClientParticleGroupManager.INSTANCE::doClientTick, false);
			AsyncTicker.registerEndTickEvent(ParticleStyleManager.INSTANCE::doTickClient, false);
			AsyncTicker.registerEndTickEvent(ParticleEmittersManager.INSTANCE::doTickClient, false);
		}
	}
}
