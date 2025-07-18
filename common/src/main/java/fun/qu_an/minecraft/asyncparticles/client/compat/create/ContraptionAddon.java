package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public interface ContraptionAddon {
	List<VoxelShape> asyncparticles$getShapes();

	List<AABB> asyncparticles$getAabbs();
}
