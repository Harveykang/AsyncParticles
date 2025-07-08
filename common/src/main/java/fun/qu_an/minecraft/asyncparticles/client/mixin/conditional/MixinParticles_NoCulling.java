package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(Particle.class) // Will be replaced with the actual targets
public abstract class MixinParticles_NoCulling implements ParticleAddon {
	@Override
	public AABB getRenderBoundingBox(float partialTick) {
		return GameUtil.infinityAABB();
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		asyncparticles$setNoCulling();
	}
}
