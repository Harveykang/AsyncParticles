//package fun.qu_an.minecraft.asyncparticles.client.mixin.create;
//import com.mojang.logging.LogUtils;
//import com.simibubi.create.content.logistics.depot.DepotBehaviour;
//import org.slf4j.Logger;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//
//import java.util.Iterator;
//import java.util.List;
//
//@Mixin(value = DepotBehaviour.class, remap = false)
//public class MixinDepotBehaviour {
//	@Unique
//	private static final Logger LOGGER = LogUtils.getLogger();
//	@Redirect(method = "insert", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
//	private boolean add(List<Object> list, Object obj) {
//		Thread thread = Thread.currentThread();
//		if (!thread.getName().startsWith("Server") && !thread.getName().startsWith("Worker")) {
//			LOGGER.error(thread.toString(), new RuntimeException("add called"));
//		}
//		return list.add(obj);
//	}
//
//	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
//	private Iterator<Object> iterator(List<Object> list) {
//		Thread thread = Thread.currentThread();
//		if (!thread.getName().startsWith("Server") && !thread.getName().startsWith("Worker")) {
//			LOGGER.error(thread.toString(), new RuntimeException("iterator called"));
//		}
//		return list.iterator();
//	}
//}
