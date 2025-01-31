package fun.qu_an.minecraft.asyncparticles.client.mixin;
import fun.qu_an.minecraft.asyncparticles.client.TickedParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public class MixinParticle implements TickedParticle {
	@Shadow protected boolean removed;
	@Unique
	private boolean ticked;

	@Override
	public boolean resetTicked() {
		return removed || !ticked || (ticked = false);
	}

	@Override
	public void setTicked() {
		this.ticked = true;
	}

	@Override
	public boolean isTicked() {
		return this.ticked;
	}
}
