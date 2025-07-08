package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_3;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.SnowParticle;

@Mixin(SnowParticle.class)
public abstract class MixinSnowParticle extends MixinWeatherParticle {
	@Unique
	private boolean asyncparticles$innvisible = false;
	protected MixinSnowParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo ci) {
		if (isAlive() && !level.getFluidState(this.pos.below()).isEmpty()) {
			asyncparticles$innvisible = true;
		}
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		if (!asyncparticles$innvisible) {
			super.render(buffer, camera, partialTick);
		}
	}
}
