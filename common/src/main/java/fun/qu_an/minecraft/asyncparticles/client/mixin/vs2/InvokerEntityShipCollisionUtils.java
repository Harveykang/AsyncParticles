package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.valkyrienskies.core.apigame.collision.ConvexPolygonc;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;

import java.util.List;

@Mixin(EntityShipCollisionUtils.class)
public interface InvokerEntityShipCollisionUtils {
	@Invoker("getShipPolygonsCollidingWithEntity")
	List<ConvexPolygonc> invoker_getShipPolygonsCollidingWithEntity(Entity e, Vec3 movement, AABB bb, Level level);
}
