package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public interface ContraptionAddon {
	Optional<VoxelShape> asyncparticles$getSimplifiedCollisionShapes();
}
