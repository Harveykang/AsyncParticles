package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(value = LevelRenderer.class, priority = 2500)
public class MixinLevelRenderer {
	@Shadow
	@Nullable
	private ClientLevel level;

	@WrapMethod(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;")
	private Particle addParticleInternal(ParticleOptions options,
										 boolean force,
										 boolean decreased,
										 double x,
										 double y,
										 double z,
										 double xSpeed,
										 double ySpeed,
										 double zSpeed,
										 Operation<Particle> original) {
		Particle particle = original.call(options, force, decreased, x, y, z, xSpeed, ySpeed, zSpeed);
		if (particle == null) {
			return null;
		}
		final ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, (int) x >> 4, (int) z >> 4);
		if (ship != null) {
			((VSParticleAddon) particle).asyncparticles$setShip(ship);
			((LightCachedParticleAddon) particle).asyncparticles$refresh();
		}
		return particle;
	}
}
