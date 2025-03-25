package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.epicfight;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.shader.AnimationShaderInstance;

@Mixin(EpicFightRenderTypes.class)
public class MixinEpicFightRenderTypes {
	@Inject(method = "getAnimationShader(Lnet/minecraft/client/renderer/ShaderInstance;)Lyesman/epicfight/client/renderer/shader/AnimationShaderInstance;",
			remap = false, at = @At("HEAD"))
	private static void getAnimationShader(ShaderInstance shaderInstance, CallbackInfoReturnable<AnimationShaderInstance> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}
