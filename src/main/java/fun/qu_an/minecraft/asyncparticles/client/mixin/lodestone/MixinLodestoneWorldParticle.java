package fun.qu_an.minecraft.asyncparticles.client.mixin.lodestone;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.lodestar.lodestone.systems.config.LodestoneConfig;
import team.lodestar.lodestone.systems.particle.world.LodestoneWorldParticle;

@Mixin(value = LodestoneWorldParticle.class, remap = false)
public class MixinLodestoneWorldParticle {
	@Redirect(method = "getVertexConsumer", at = @At(value = "INVOKE" , target = "Lteam/lodestar/lodestone/systems/config/LodestoneConfig$ConfigValueHolder;getConfigValue()Ljava/lang/Object;"))
	private Object getVertexConsumerConfigValue(LodestoneConfig.ConfigValueHolder<Boolean> instance) {
		return false;
	}
}
