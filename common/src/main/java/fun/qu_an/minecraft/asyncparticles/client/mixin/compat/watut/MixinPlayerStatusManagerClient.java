package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.watut;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.client.CustomParticleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerStatusManagerClient.class)
public class MixinPlayerStatusManagerClient {
	@Redirect(method = "receiveItemMove", at = @At(value = "INVOKE", target = "Lcom/corosus/watut/client/CustomParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
	private void onAddParticle(CustomParticleEngine instance, Particle p_107345_) {
		Minecraft.getInstance().particleEngine.add(p_107345_);
	}
}
