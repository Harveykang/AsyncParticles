package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionHeightMapProvider;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel implements ContraptionHeightMapProvider {
	@Unique
	private final Long2FloatMap asyncparticles$contraptionHeightMap = new Long2FloatOpenCustomHashMap(new LongHash.Strategy() {
		@Override
		public int hashCode(long e) {
			return BlockPos.getX(e) * 31 ^ BlockPos.getZ(e);
		}

		@Override
		public boolean equals(long a, long b) {
			return a == b;
		}
	});
	@Unique
	private final Long2BooleanMap asyncparticles$contraptionMoving = new Long2BooleanOpenHashMap();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		asyncparticles$contraptionHeightMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
		asyncparticles$contraptionMoving.defaultReturnValue(false);
	}

	@Override
	public Long2FloatMap asyncparticles$getHeightMap() {
		return asyncparticles$contraptionHeightMap;
	}

	@Override
	public Long2BooleanMap asyncparticles$getMovingMap() {
		return asyncparticles$contraptionMoving;
	}
}
