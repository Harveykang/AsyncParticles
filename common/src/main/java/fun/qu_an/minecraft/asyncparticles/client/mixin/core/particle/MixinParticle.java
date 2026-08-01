package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.FrustumUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon, GpuParticleAddon, LightCachedParticleAddon {
	@Unique
	private static final int RENDER_FLAG_SYNC = 1;
	@Unique
	private static final int RENDER_FLAG_ENABLE_CULL = 2;
	@Unique
	private static final int RENDER_FLAG_SHOULD_CULL = 4;

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
	private volatile boolean asyncparticles$ticked = true; // always true at first tick
	@Unique
	private byte asyncparticles$renderFlag = RENDER_FLAG_ENABLE_CULL;
	@Unique
	private boolean asyncparticles$enableLightCache = false;

	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
	protected void onInit(CallbackInfo ci) {
		Class<?> aClass = asyncparticles$getRealClass();
		if (AsyncRenderBehavior.getInstance().shouldSync(aClass)) {
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
		asyncparticles$renderFlag |= RENDER_FLAG_SYNC;
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return (asyncparticles$renderFlag & RENDER_FLAG_SYNC) != 0;
	}

	@Override
	public void asyncparticles$enableLightCache(boolean b) {
		asyncparticles$enableLightCache = b;
	}

	public boolean asyncparticles$isEnabledLightCache() {
		return asyncparticles$enableLightCache;
	}

	public boolean asyncparticles$shouldCull() {
		return (asyncparticles$renderFlag & RENDER_FLAG_ENABLE_CULL) != 0;
	}

	public void asyncparticles$setNoCulling() {
		asyncparticles$renderFlag &= ~RENDER_FLAG_ENABLE_CULL;
	}

	public boolean asyncparticles$isVisibleOnScreen() {
		return (asyncparticles$renderFlag & RENDER_FLAG_SHOULD_CULL) != 0;
	}

	public void asyncparticles$tickAABBCulling() {
		AABB aabb = getRenderBoundingBox(0f).expandTowards(xd, yd, zd);
		if (FrustumUtil.isVisible(AsyncRenderBehavior.getInstance().getFrustum(), aabb)) {
			asyncparticles$renderFlag |= RENDER_FLAG_SHOULD_CULL;
		} else {
			asyncparticles$renderFlag &= ~RENDER_FLAG_SHOULD_CULL;
		}
	}

	public void asyncparticles$tickSphereCulling() {
		if (FrustumUtil.isVisible(AsyncRenderBehavior.getInstance().getFrustum(), (Particle) (Object) this)) {
			asyncparticles$renderFlag |= RENDER_FLAG_SHOULD_CULL;
		} else {
			asyncparticles$renderFlag &= ~RENDER_FLAG_SHOULD_CULL;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class) this.getClass();
	}
}
