package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.ParticleSpawner;

@Mixin(ParticleSpawner.class)
public class MixinParticleSpawner {
	@Unique
	private static final ResourceLocation asyncparticles$PARTICLE_RAIN$TICK =
		new ResourceLocation("particlerain", "tick");

	@WrapMethod(method = "tick")
	private static void wrapTick(ClientLevel level, Vec3 cameraPos, Operation<Void> original) {
		EndTickOperation.schedule(asyncparticles$PARTICLE_RAIN$TICK, () -> original.call(level, cameraPos));
	}

	@WrapWithCondition(method = "tickBlockFX", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
	private static boolean onTickBlockFX(ParticleEngine instance, Particle particle) {
		return particle != null;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/ParticleSpawner;particleCount:I", opcode = Opcodes.GETSTATIC))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.particleCount.get();
	}
}
