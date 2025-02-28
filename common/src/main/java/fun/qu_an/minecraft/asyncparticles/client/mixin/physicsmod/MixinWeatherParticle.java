package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import net.diebuddies.minecraft.weather.FastTextureSheetParticle;
import net.diebuddies.minecraft.weather.WeatherParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends FastTextureSheetParticle {
	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

//	@Inject(method = "tick",
//		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
//		cancellable = true)
//	private void onTick(CallbackInfo ci) {
//		if (PhysicsModVSClientUtils.collideWithShip(level, x, y, z, aabb)) {
//			remove();
//			ci.cancel();
//		}
//	}
}
