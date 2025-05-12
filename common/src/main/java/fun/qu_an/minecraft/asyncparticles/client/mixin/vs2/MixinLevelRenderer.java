package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

// FIXME: This is unstable
@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer {
	@Shadow
	@Nullable
	private ClientLevel level;

	@Inject(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
		at = @At("HEAD"))
	private void addParticleInternal(ParticleOptions options,
									 boolean force,
									 boolean decreased,
									 double x,
									 double y,
									 double z,
									 double xSpeed,
									 double ySpeed,
									 double zSpeed,
									 CallbackInfoReturnable<Particle> cir) {
		final ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, (int) x >> 4, (int) z >> 4);
		if (ship != null) {
			Particle particle = cir.getReturnValue();
			((VSParticleAddon) particle).asyncparticles$setShip(ship);
			((LightCachedParticleAddon) particle).asyncparticles$refresh();
		}
	}
}
