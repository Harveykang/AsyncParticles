package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.immersive_portals;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.ParticleRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;

@Mixin(value = ParticleRenderer.class)
public class MixinParticleRenderer {
	@WrapOperation(method = {"tick", "append"}, at = @At(value = "INVOKE", target = "Lfun/qu_an/minecraft/asyncparticles/client/addon/GpuParticleAddon;asyncparticles$shouldRender()Z"))
	private boolean shouldRender(GpuParticleAddon instance, Operation<Boolean> original) {
		if (!original.call(instance)) {
			return false;
		}
		IEParticle ie = (IEParticle) instance;
		return ie.portal_getWorld() == Minecraft.getInstance().player.level();
	}
}
