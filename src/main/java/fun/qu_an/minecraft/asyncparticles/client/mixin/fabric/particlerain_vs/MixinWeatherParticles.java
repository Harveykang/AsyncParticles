package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.*;

@Mixin(value = {DustParticle.class,
	RainParticle.class,
	FogParticle.class,
	GroundFogParticle.class,
	StreakParticle.class,
	ShrubParticle.class,
	RippleParticle.class}, remap = false)
public abstract class MixinWeatherParticles extends MixinWeatherPatricle {

	protected MixinWeatherParticles(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void onRender(VertexConsumer vertexConsumer, Camera camera, float tickPercentage, CallbackInfo ci) {
		if (invisible) {
			ci.cancel();
		}
	}
}
