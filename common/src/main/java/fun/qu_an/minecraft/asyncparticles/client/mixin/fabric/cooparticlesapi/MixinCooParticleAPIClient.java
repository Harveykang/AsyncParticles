package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.cooparticlesapi;

import cn.coostack.cooparticlesapi.CooParticleAPIClient;
import cn.coostack.cooparticlesapi.network.particle.emitters.ParticleEmittersManager;
import cn.coostack.cooparticlesapi.network.particle.style.ParticleStyleManager;
import cn.coostack.cooparticlesapi.particles.control.group.ClientParticleGroupManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickEvent;
import fun.qu_an.minecraft.asyncparticles.client.compat.cooparticlesapi.CooTickMode;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CooParticleAPIClient.class)
public class MixinCooParticleAPIClient {
	@Dynamic
	@WrapWithCondition(method = {"onInitializeClient", "registerClientEvents"}, remap = false,
		at = @At(value = "INVOKE", remap = false, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private boolean onRegisterEvent(Event<ClientTickEvents.StartWorldTick> instance, Object t) {
		if (!(t instanceof ClientTickEvents.StartWorldTick event)) {
			return true;
		}

		instance.register(mc -> {
			if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.SYNCHRONOUSLY) {
				event.onStartTick(mc);
			}
		});
		EndTickEvent.register(() -> {
			switch (ConfigHelper.cooparticlesapi$getTickMode()) {
				case ASYNC_IN_PARALLEL -> ClientParticleGroupManager.INSTANCE.doClientTick();
				case ASYNC_IN_SEQUENCED -> event.onStartTick(null);
			}
		});
		EndTickEvent.register(() -> {
			if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.ASYNC_IN_PARALLEL) {
				ParticleStyleManager.INSTANCE.doTickClient();
			}
		});
		EndTickEvent.register(() -> {
			if (ConfigHelper.cooparticlesapi$getTickMode() == CooTickMode.ASYNC_IN_PARALLEL) {
				ParticleEmittersManager.INSTANCE.doTickClient();
			}
		});

		return false;
	}
}
