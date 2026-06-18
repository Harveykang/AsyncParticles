package fun.qu_an.minecraft.asyncparticles.client.mixin.accessor;

import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrustumIntersection.class)
public interface AccessorFrustumIntersection {
	@Accessor
	float getNxX();
	@Accessor
	float getNxY();
	@Accessor
	float getNxZ();
	@Accessor
	float getNxW();
	@Accessor
	float getPxX();
	@Accessor
	float getPxY();
	@Accessor
	float getPxZ();
	@Accessor
	float getPxW();
	@Accessor
	float getNyX();
	@Accessor
	float getNyY();
	@Accessor
	float getNyZ();
	@Accessor
	float getNyW();
	@Accessor
	float getPyX();
	@Accessor
	float getPyY();
	@Accessor
	float getPyZ();
	@Accessor
	float getPyW();
	@Accessor
	float getNzX();
	@Accessor
	float getNzY();
	@Accessor
	float getNzZ();
	@Accessor
	float getNzW();
	@Accessor
	float getPzX();
	@Accessor
	float getPzY();
	@Accessor
	float getPzZ();
	@Accessor
	float getPzW();
}
