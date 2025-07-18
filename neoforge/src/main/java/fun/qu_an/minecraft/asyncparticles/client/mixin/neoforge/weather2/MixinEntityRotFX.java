package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.weather2;

import extendedrenderer.ParticleManagerExtended;
import extendedrenderer.particle.entity.EntityRotFX;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(EntityRotFX.class)
public class MixinEntityRotFX {
	@Redirect(method = "spawnAsWeatherEffect", remap = false,
		at = @At(value = "INVOKE", target = "Lextendedrenderer/ParticleManagerExtended;add(Lnet/minecraft/client/particle/Particle;)V"))
	private void onSpawnAsWeatherEffect(ParticleManagerExtended instance, Particle p_107345_) {

		Minecraft.getInstance().particleEngine.add(p_107345_); // never use ParticleManagerExtended.add()...
	}

	@Redirect(method = "remove", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"))
	private boolean onRemove(List<Particle> instance, Object p_107347_) {
		if (ThreadUtil.isOnMainThread()) {
			return instance.remove(p_107347_);
		}
		ThreadUtil.enqueueClientTask(() -> instance.remove((Particle) p_107347_));
		return true;
	}
}
