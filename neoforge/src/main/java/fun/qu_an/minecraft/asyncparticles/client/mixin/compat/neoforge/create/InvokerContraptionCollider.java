package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ContraptionCollider.class)
public interface InvokerContraptionCollider {
	@Invoker(value = "getPotentiallyCollidedShapes", remap = false)
	static void invoker_getPotentiallyCollidedShapes(Level world, Contraption contraption, AABB localBB, Shapes.DoubleLineConsumer out) {
		throw new AssertionError();
	}
}
