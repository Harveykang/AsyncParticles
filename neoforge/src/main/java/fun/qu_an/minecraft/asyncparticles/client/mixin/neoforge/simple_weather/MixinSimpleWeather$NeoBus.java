package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tv.soaryn.simpleweather.SimpleWeather;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus", remap = false)
public interface MixinSimpleWeather$NeoBus {
	@Inject(method = {"addRain", "addSnowflake"}, cancellable = true,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
	private static void onAddRain(ClientLevel level, int x, int y, int z, BlockPos.MutableBlockPos pos, int range, CallbackInfo ci) {
		if (CreateCompat.isUnderContraption(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			ci.cancel();
		}
	}

	@WrapMethod(method = "renderWeather")
	private static void renderWeather(ClientTickEvent.Pre event, Operation<Void> original) {
		if (SimpleWeather.ClientConfig.OverrideWeather.get()) {
			AsyncTicker.addEndTickTask(() -> original.call((Object) null));
		}
	}
}
