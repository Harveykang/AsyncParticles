package fun.qu_an.minecraft.asyncparticles.client.mixin;
import fun.qu_an.minecraft.asyncparticles.client.TickedParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;

@Mixin(Particle.class)
public abstract class MixinParticle implements TickedParticle {
	@Shadow protected boolean removed;

	@Shadow public abstract void remove();

	@Shadow public abstract boolean isAlive();

	@Shadow public double x;
	@Shadow public double y;
	@Shadow public double z;
	@Shadow @Final public ClientLevel level;
	@Unique
	private boolean asyncParticles$ticked;

	@Override
	public boolean asyncParticles$shouldRemove() {
		if (!isAlive()) return true;
		if (asyncParticles$ticked) return asyncParticles$ticked = false;
		remove();
		return true;
	}

	@Override
	public void asyncParticles$setTicked() {
		this.asyncParticles$ticked = true;
	}

	@Override
	public boolean asyncParticles$isTicked() {
		return this.asyncParticles$ticked;
	}
}
