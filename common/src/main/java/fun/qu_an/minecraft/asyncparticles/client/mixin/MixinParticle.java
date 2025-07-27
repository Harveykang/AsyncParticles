package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
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
	public ClientLevel level;
	@Unique
	private boolean asyncparticles$ticked = true;
	@Unique
	private byte asyncparticles$renderFlag = 2;
	@Unique
	private byte asyncparticles$tickFlag;

	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
	protected void onInit(CallbackInfo ci) {
		Class<?> aClass = asyncparticles$getRealClass();
		if (AsyncTicker.shouldSync(aClass)) {
			asyncparticles$setTickSync();
		}
		if (AsyncRenderer.shouldSync(aClass)) {
			asyncparticles$setRenderSync();
		}
	}

	@Shadow
	protected double xd;

	@Shadow
	protected double yd;

	@Shadow
	protected double zd;

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

	public void asyncparticles$enableLightCache() {
		asyncparticles$tickFlag |= 2;
	}

	public void asyncparticles$disableLightCache() {
		asyncparticles$tickFlag &= ~2;
	}

	public boolean asyncparticles$isEnabledLightCache() {
		return (asyncparticles$tickFlag & 2) != 0;
	}

	public boolean asyncparticles$shouldCull() {
		return (asyncparticles$renderFlag & 2) != 0;
	}

	public void asyncparticles$setNoCulling() {
		asyncparticles$renderFlag &= ~2;
	}

	public boolean asyncparticles$isVisibleOnScreen() {
		return (asyncparticles$renderFlag & 4) != 0;
	}

	public void asyncparticles$tickAABBCulling() {
		AABB aabb = getRenderBoundingBox(0f).expandTowards(xd, yd, zd);
		if (FrustumUtil.isVisible(AsyncRenderer.frustum, aabb)) {
			asyncparticles$renderFlag |= 4;
		} else {
			asyncparticles$renderFlag &= ~4;
		}
	}

	public void asyncparticles$tickSphereCulling() {
		if (FrustumUtil.isVisible(AsyncRenderer.frustum, (Particle) (Object) this)) {
			asyncparticles$renderFlag |= 4;
		} else {
			asyncparticles$renderFlag &= ~4;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}
}
