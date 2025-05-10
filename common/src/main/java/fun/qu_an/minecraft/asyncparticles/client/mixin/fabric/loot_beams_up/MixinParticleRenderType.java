package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.loot_beams_up;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.lootbeams.render.ParticleRenderType")
public class MixinParticleRenderType {
	@Shadow(remap = false) private ResourceLocation texture;

	@Dynamic
	@Inject(method = "<init>", remap = false, at = @At("RETURN"))
	private void onInit(CallbackInfo ci){
		texture = TextureAtlas.LOCATION_PARTICLES;
	}
}
