package fun.qu_an.minecraft.asyncparticles.client.mixin.lodestone;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.lodestar.lodestone.systems.particle.world.LodestoneWorldParticle;

@Mixin(LodestoneWorldParticle.class)
public class MixinLodestoneWorldParticle {
	@Inject(method = "getVertexConsumer", at = @At(value = "INVOKE", target = "Lteam/lodestar/lodestone/handlers/RenderHandler$LodestoneRenderLayer;getParticleTarget()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
	private void onGetVertexConsumer(CallbackInfoReturnable<VertexConsumer> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}
