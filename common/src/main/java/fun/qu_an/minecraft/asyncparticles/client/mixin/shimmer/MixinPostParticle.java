package fun.qu_an.minecraft.asyncparticles.client.mixin.shimmer;

import com.lowdragmc.shimmer.client.postprocessing.PostParticle;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticle;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostParticle.class)
public abstract class MixinPostParticle extends MixinParticle implements LightCachedParticleAddon {
	@Shadow
	@Final
	public Particle parent;

	@Inject(method = "tick", at = @At("RETURN"))
	protected void onTick(CallbackInfo ci) {
		if (ConfigHelper.getParticleCullingMode() == ParticleCullingMode.SPHERE){
			x = parent.x;
			y = parent.y;
			z = parent.z;
			bbWidth = parent.bbWidth;
			bbHeight = parent.bbHeight;
		}
	}

	@Override
	protected void onInit(CallbackInfo ci) {
	}

	@Override
	public int getLightColor(float partialTick) {
		return parent.getLightColor(partialTick);
	}

	@Override
	public void asyncparticles$setLight(int light) {
		((LightCachedParticleAddon) parent).asyncparticles$setLight(light);
	}

	@Override
	public byte asyncparticles$getCompressedLight() {
		return ((LightCachedParticleAddon) parent).asyncparticles$getCompressedLight();
	}

	@Override
	public void asyncparticles$refresh() {
		((LightCachedParticleAddon) parent).asyncparticles$refresh();
	}

	@Override
	public int asyncparticles$invoke_getLightColor(float partialTick) {
		return parent.getLightColor(partialTick);
	}

	@Override
	public void asyncparticles$setTicked() {
		((ParticleAddon) parent).asyncparticles$setTicked();
	}

	@Override
	public void asyncparticles$resetTicked() {
		((ParticleAddon) parent).asyncparticles$resetTicked();
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return ((ParticleAddon) parent).asyncparticles$isTicked();
	}

	@Override
	public void asyncparticles$setRenderSync() {
		((ParticleAddon) parent).asyncparticles$setRenderSync();
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return ((ParticleAddon) parent).asyncparticles$isRenderSync();
	}

	@Override
	public void asyncparticles$setTickSync() {
		((ParticleAddon) parent).asyncparticles$setTickSync();
	}

	@Override
	public boolean asyncparticles$isTickSync() {
		return ((ParticleAddon) parent).asyncparticles$isTickSync();
	}

	@Override
	public boolean shouldCull() {
		return ((ParticleAddon) parent).shouldCull();
	}

	public boolean asyncparticles$isVisibleOnScreen() {
		return ((ParticleAddon) parent).asyncparticles$isVisibleOnScreen();
	}

	public void asyncparticles$tickAABBCulling() {
		((ParticleAddon) parent).asyncparticles$tickAABBCulling();
	}

	public void asyncparticles$tickSphereCulling() {
		((ParticleAddon) parent).asyncparticles$tickSphereCulling();
	}

	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return ((ParticleAddon) parent).asyncparticles$getRealClass();
	}
}
