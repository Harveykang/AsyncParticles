//package fun.qu_an.minecraft.asyncparticles.client.mixin.a_good_place;
//
//import net.minecraft.core.BlockPos;
//import nl.enjarai.a_good_place.particles.BlocksParticlesManager;
//import nl.enjarai.a_good_place.particles.PlacingBlockParticle;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Mutable;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Mixin(BlocksParticlesManager.class)
//public class MixinBlocksParticlesManager {
//	@Mutable
//	@Shadow(remap = false) @Final protected static Map<BlockPos, PlacingBlockParticle> PARTICLES;
//
//	@Inject(method = "<clinit>", at = @At("RETURN"))
//	private static void onInit(CallbackInfo ci) {
//		PARTICLES = new ConcurrentHashMap<>();
//	}
//}
