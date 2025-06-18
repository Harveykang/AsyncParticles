package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particlerain;

import com.leclowndu93150.particlerain.particle.RainParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RainParticle.class)
public abstract class MixinRainParticle extends MixinWeatherParticle {
	@Unique
	private boolean asyncparticles$innvisible = false;
	protected MixinRainParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo ci) {
		if (isAlive() && !level.getFluidState(this.pos.below(2)).isEmpty()) {
			asyncparticles$innvisible = true;
		}
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	public void render(VertexConsumer vertexConsumer, Camera camera, float tickPercentage, CallbackInfo ci) {
		if (asyncparticles$innvisible) {
			ci.cancel();
		}
	}
}
