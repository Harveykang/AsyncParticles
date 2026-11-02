package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MolangParticleEngine.class, remap = false)
public abstract class MixinMolangParticleEngine implements ParticleStormEngineAddon {
	@Inject(method = "tick", at = @At("TAIL"))
	private void publishTick(Minecraft minecraft, LocalPlayer player, CallbackInfo ci) {
		if (ConfigHelper.isParticleStormGpuRendering()) {
			ParticleStormCompat.afterTick(asyncparticles$getGroupedParticles(), minecraft.gameRenderer.getMainCamera());
		}
	}

	@Inject(method = "removeAll", at = @At("HEAD"))
	private void releaseRenderer(CallbackInfo ci) {
		ParticleStormCompat.release();
	}
}
