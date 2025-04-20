package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 9000)
public abstract class MixinParticleEngine_Late {
	@Mutable
	@Shadow
	public static List<ParticleRenderType> RENDER_ORDER;

	@Shadow @Final public TextureManager textureManager;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		List<ParticleRenderType> renderTypes = new ArrayList<>(RENDER_ORDER.size());
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) != AsyncRenderer.EMPTY_FORMAT) {
				renderTypes.add(type);
			}
		}
		for (ParticleRenderType type : RENDER_ORDER) {
			if (AsyncRenderer.getVertexFormatPair(type, textureManager) == AsyncRenderer.EMPTY_FORMAT) {
				renderTypes.add(type);
			}
		}
		RENDER_ORDER = renderTypes;
	}
}
