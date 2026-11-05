package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleTypeAddon;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomParticleInstance.class)
public abstract class MixinCustomParticleInstance extends TextureSheetParticle implements PolytoneParticleAddon, ParticleAddon, GpuParticleAddon {
	protected MixinCustomParticleInstance(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Shadow(remap = false)
	@Final
	public CustomParticleType type;
	@Unique
	private boolean asyncparticles$lockTickAndRender;
	@Shadow(remap = false)
	private float oAlpha;
	@Shadow(remap = false)
	private float oBCol;
	@Shadow(remap = false)
	private float oGCol;
	@Shadow(remap = false)
	private float oRCol;

	@Shadow
	public abstract ParticleRenderType getRenderType();

	@Override
	public boolean asyncparticles$canRenderFast() {
		return ((Object) this).getClass() == CustomParticleInstance.class
			&& getRenderType() != ParticleRenderType.CUSTOM
			&& ((PolytoneParticleTypeAddon) type).asyncparticles$isSimpleQuad();
	}

	@Override
	public boolean asyncparticles$shouldLockTickAndRender() {
		return asyncparticles$lockTickAndRender;
	}

	@Override
	public boolean asyncparticles$isStaticLight() {
		return true;
	}

	@Override
	public double asyncparticles$getXo() {
		return xo + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().x;
	}

	@Override
	public double asyncparticles$getYo() {
		return yo + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().y;
	}

	@Override
	public double asyncparticles$getZo() {
		return zo + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().z;
	}

	@Override
	public double asyncparticles$getX() {
		return x + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().x;
	}

	@Override
	public double asyncparticles$getY() {
		return y + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().y;
	}

	@Override
	public double asyncparticles$getZ() {
		return z + ((PolytoneParticleTypeAddon) type).asyncparticles$getWorldOffset().z;
	}

	@Override
	public int asyncparticles$getOColor() {
		return FastColor.ABGR32.color(
			(int) (oAlpha * 255.0f) & 255,
			(int) (oBCol * 255.0f) & 255,
			(int) (oGCol * 255.0f) & 255,
			(int) (oRCol * 255.0f) & 255);
	}

	@Override
	public int asyncparticles$getColor(int oColor) {
		return FastColor.ABGR32.color(
			(int) (alpha * 255.0f) & 255,
			(int) (bCol * 255.0f) & 255,
			(int) (gCol * 255.0f) & 255,
			(int) (rCol * 255.0f) & 255);
	}

	@ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD",
		target = "Lnet/mehvahdjukaar/polytone/content/particle/custom/CustomParticleInstance;STATE_HACK:Lnet/minecraft/world/level/block/state/BlockState;",
		opcode = Opcodes.GETSTATIC))
	private BlockState getSpawnState(BlockState original) {
		return PolytoneCompat.SPAWN_STATE.get();
	}

	@Inject(method = "setStateHack", at = @At("HEAD"), cancellable = true, remap = false)
	private static void setSpawnState(BlockState state, CallbackInfo ci) {
		PolytoneCompat.SPAWN_STATE.set(state);
		ci.cancel();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void preserveCustomGeometry(CallbackInfo ci) {
		asyncparticles$lockTickAndRender = PolytoneCompat.shouldLock(this);
		PolytoneParticleTypeAddon typeAddon = (PolytoneParticleTypeAddon) type;
		if (getRenderType() == ParticleRenderType.CUSTOM || !typeAddon.asyncparticles$isSimpleQuad()
			|| !Vec3.ZERO.equals(typeAddon.asyncparticles$getWorldOffset())) {
			this.asyncparticles$setNoCulling();
		}
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
	private float clampColor(float color) {
		return Mth.clamp(color, 0f, 1f);
	}
}
