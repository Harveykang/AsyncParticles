package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.leclowndu93150.particlerain.particle.*;

@Mixin(value = {
	DustParticle.class,
	RainParticle.class,
	FogParticle.class,
	GroundFogParticle.class,
	StreakParticle.class,
	ShrubParticle.class,
	RippleParticle.class})
public abstract class MixinWeatherParticles extends MixinWeatherParticle {

	protected MixinWeatherParticles(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void onRender(VertexConsumer vertexConsumer, Camera camera, float tickPercentage, CallbackInfo ci) {
		if (asyncparticles$invisible()) {
			ci.cancel();
		}
	}
}
