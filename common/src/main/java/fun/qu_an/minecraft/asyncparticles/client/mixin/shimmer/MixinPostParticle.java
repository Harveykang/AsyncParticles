package fun.qu_an.minecraft.asyncparticles.client.mixin.shimmer;

import com.lowdragmc.shimmer.client.postprocessing.PostParticle;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostParticle.class)
public abstract class MixinPostParticle extends MixinParticle implements LightCachedParticleAddon {
	@Shadow
	@Final
	public Particle parent;

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

	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return ((ParticleAddon) parent).asyncparticles$getRealClass();
	}
}
