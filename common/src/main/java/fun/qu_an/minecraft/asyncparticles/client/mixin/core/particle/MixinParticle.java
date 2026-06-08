package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon, LightCachedParticleAddon {
	@Shadow
	public abstract void remove();

	@Shadow
	public double x;
	@Shadow
	public double y;
	@Shadow
	public double z;
	@Shadow
	@Final
	protected ClientLevel level;
	@Unique
	private boolean asyncparticles$ticked = true; // always true at first tick
	@Unique
	private byte asyncparticles$renderFlag = 2;
	@Unique
	private byte asyncparticles$tickFlag;

	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
	protected void onInit(CallbackInfo ci) {
		Class<?> aClass = asyncparticles$getRealClass();
		if (AsyncTickBehavior.getInstance().shouldSync(aClass)) {
			asyncparticles$setTickSync();
		}
//		if (AsyncRenderBehavior.shouldSync(aClass)) {
//			asyncparticles$setRenderSync();
//		}
	}

	@Override
	public void asyncparticles$setTicked() {
		this.asyncparticles$ticked = true;
	}

	@Override
	public void asyncparticles$resetTicked() {
		this.asyncparticles$ticked = false;
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return this.asyncparticles$ticked;
	}

	@Override
	public void asyncparticles$setRenderSync() {
		asyncparticles$renderFlag |= 1;
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return (asyncparticles$renderFlag & 1) != 0;
	}

	@Override
	public void asyncparticles$setTickSync() {
		asyncparticles$tickFlag |= 1;
	}

	@Override
	public boolean asyncparticles$isTickSync() {
		return (asyncparticles$tickFlag & 1) != 0;
	}

	@Override
	public void asyncparticles$enableLightCache() {
		asyncparticles$tickFlag |= 2;
	}

	@Override
	public void asyncparticles$disableLightCache() {
		asyncparticles$tickFlag &= ~2;
	}

	@Override
	public boolean asyncparticles$isEnabledLightCache() {
		return (asyncparticles$tickFlag & 2) != 0;
	}

	@Override
	public boolean asyncparticles$isVisibleOnScreen() {
		return (asyncparticles$renderFlag & 4) != 0;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}
}
