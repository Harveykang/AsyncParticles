package fun.qu_an.minecraft.asyncparticles.client.mixin.create;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ContraptionCollider.class)
public class InvokerContraptionCollider {
	@Invoker("getPotentiallyCollidedShapes")
	public static List<VoxelShape> invoker_getPotentiallyCollidedShapes(Level world, Contraption contraption, AABB localBB) {
		throw new AssertionError();
	}
}
