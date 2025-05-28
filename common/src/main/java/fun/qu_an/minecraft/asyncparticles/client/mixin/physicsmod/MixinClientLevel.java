package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.diebuddies.minecraft.ParticleSpawner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientLevel.class, priority = 1100)
public class MixinClientLevel {
	@TargetHandler(
		name = "addParticle",
		mixin = "net.diebuddies.mixins.MixinClientLevel"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
		target = "Lnet/diebuddies/minecraft/ParticleSpawner;spawnServerBlockPhysicsParticle(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;DDDDDD)V"))
	public void addParticle(BlockState state, Level level, double x, double y, double z, double vx, double vy, double vz) {
		if (ThreadUtil.isOnMainThread()) {
			ParticleSpawner.spawnServerBlockPhysicsParticle(state, level, x, y, z, vx, vy, vz);
		} else {
			ThreadUtil.enqueueClientTask(() -> ParticleSpawner.spawnServerBlockPhysicsParticle(state, level, x, y, z, vx, vy, vz));
		}
	}
}
