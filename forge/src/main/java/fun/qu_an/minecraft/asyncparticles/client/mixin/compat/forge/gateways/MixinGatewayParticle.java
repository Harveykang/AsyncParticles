package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.gateways;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.shadowsoffire.gateways.client.GatewayParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// fix low performance
@Mixin(GatewayParticle.class)
public abstract class MixinGatewayParticle extends TextureSheetParticle {
	@Shadow(remap = false)
	@Final
	static ParticleRenderType RENDER_TYPE;

	protected MixinGatewayParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@WrapOperation(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
	private void onBeginRender(ParticleRenderType instance, BufferBuilder builder, TextureManager textureManager, Operation<Void> original) {
		// do nothing
	}

	@WrapOperation(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V"))
	private void onEndRender(ParticleRenderType instance, Tesselator tesselator, Operation<Void> original) {
		// do nothing
	}

	@Redirect(method = "getRenderType", require = 0, at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleRenderType;CUSTOM:Lnet/minecraft/client/particle/ParticleRenderType;"))
	private ParticleRenderType onGetRenderType() {
		return RENDER_TYPE;
	}
}
