//package fun.qu_an.minecraft.asyncparticles.client.mixin.a_good_place;
//
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.particle.Particle;
//import net.minecraft.core.BlockPos;
//import nl.enjarai.a_good_place.particles.BlocksParticlesManager;
//import nl.enjarai.a_good_place.particles.PlacingBlockParticle;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//
//@Mixin(PlacingBlockParticle.class)
//public abstract class MixinPlacingBlockParticle extends Particle {
//	@Shadow @Final protected BlockPos pos;
//
//	protected MixinPlacingBlockParticle(ClientLevel level, double x, double y, double z) {
//		super(level, x, y, z);
//	}
//
//	@Override
//	public void remove() {
//		if (isAlive()) {
//			BlocksParticlesManager.unHideBlock(this.pos);
//		}
//		super.remove();
//	}
//}
