package fun.qu_an.minecraft.asyncparticles.client.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(Contraption.class)
public class MixinContraption implements ContraptionAddon {
	@Shadow(remap = false)
	protected Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks;
	@Shadow(remap = false)
	protected ContraptionWorld world;
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@Shadow(remap = false) public Optional<List<AABB>> simplifiedEntityColliders;
	@Unique
	private List<AABB> asyncparticles$aabbs;
	@Unique
	private final Object asyncparticles$lock = new Object();

	@WrapOperation(method = "gatherBBsOffThread", remap = false, at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Void> gatherBBsOffThread(CompletableFuture<?> instance, Consumer<?> action, Operation<CompletableFuture<Void>> original) {
		return original.call(instance, action).thenRun(() -> {
			asyncparticles$aabbs = null;
		});
	}

	@Override
	public List<AABB> asyncparticles$getAabbs() {
		if (simplifiedEntityColliders.isPresent()) {
			return simplifiedEntityColliders.get();
		}
		if (asyncparticles$aabbs != null) {
			return asyncparticles$aabbs;
		}
		synchronized (asyncparticles$lock){
			if (asyncparticles$aabbs != null) {
				return asyncparticles$aabbs;
			}
			List<AABB> aabbs = new ArrayList<>();
			for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : blocks.entrySet()) {
				StructureTemplate.StructureBlockInfo info = entry.getValue();
				BlockPos localPos = entry.getKey();
				VoxelShape collisionShape = info.state().getCollisionShape(this.world, localPos, CollisionContext.empty());
				if (!collisionShape.isEmpty()) {
					aabbs.addAll(collisionShape.move(localPos.getX(), localPos.getY(), localPos.getZ()).toAabbs());
				}
			}
			return asyncparticles$aabbs = aabbs;
		}
	}
}
