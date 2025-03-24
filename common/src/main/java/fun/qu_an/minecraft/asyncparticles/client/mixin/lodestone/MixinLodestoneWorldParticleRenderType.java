package fun.qu_an.minecraft.asyncparticles.client.mixin.lodestone;

import com.google.common.collect.ImmutableList;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

@Mixin(value = LodestoneWorldParticleRenderType.class)
public class MixinLodestoneWorldParticleRenderType {
	@Inject(method = "addDepthFade", remap = false, at = @At("RETURN"))
	private static void addDepthFade(LodestoneWorldParticleRenderType original, CallbackInfoReturnable<LodestoneWorldParticleRenderType> cir) {
		if (ModListHelper.IS_FORGE) {
			return;
		}
		ParticleEngine.RENDER_ORDER = ImmutableList.<ParticleRenderType>builder()
			.addAll(ParticleEngine.RENDER_ORDER)
			.add(cir.getReturnValue())
			.build();
	}

//	@Inject(method = "end", at = @At("HEAD"))
//	private void end(Tesselator pTesselator, CallbackInfo ci) {
//		pTesselator.getBuilder().setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN);
//	}
}
