package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pigcart.particlerain.WeatherParticleManager;

@Mixin(WeatherParticleManager.class)
public class MixinWeatherParticleManager {
	@Unique
	private static final ResourceLocation asyncparticles$PARTICLE_RAIN$TICK =
		ResourceLocation.fromNamespaceAndPath("particlerain", "tick");

	@WrapMethod(method = "tick")
	private static void wrapTick(ClientLevel level, Vec3 cameraPos, Operation<Void> original) {
		EndTickOperation.schedule(asyncparticles$PARTICLE_RAIN$TICK, () -> original.call(level, cameraPos));
	}
}
